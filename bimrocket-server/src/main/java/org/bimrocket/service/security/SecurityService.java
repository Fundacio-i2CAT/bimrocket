/*
 * BIMROCKET
 *
 * Copyright (C) 2021-2025, Ajuntament de Sant Feliu de Llobregat
 *
 * This program is licensed and may be used, modified and redistributed under
 * the terms of the European Public License (EUPL), either version 1.1 or (at
 * your option) any later version as soon as they are approved by the European
 * Commission.
 *
 * Alternatively, you may redistribute and/or modify this program under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either  version 3 of the License, or (at your option)
 * any later version.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the licenses for the specific language governing permissions, limitations
 * and more details.
 *
 * You should have received a copy of the EUPL1.1 and the LGPLv3 licenses along
 * with this program; if not, you may find them at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 * http://www.gnu.org/licenses/
 * and
 * https://www.gnu.org/licenses/lgpl.txt
 */
package org.bimrocket.service.security;

import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.NewCookie;
import org.apache.commons.lang3.StringUtils;
import org.bimrocket.api.security.Role;
import org.bimrocket.api.security.User;
import org.bimrocket.dao.Dao;
import org.bimrocket.exception.InvalidRequestException;
import org.bimrocket.exception.NotAuthorizedException;
import org.bimrocket.exception.NotFoundException;
import org.bimrocket.service.security.store.SecurityDaoStore;
import org.bimrocket.service.security.store.empty.SecurityEmptyDaoStore;
import org.bimrocket.util.JWTUtils;
import org.eclipse.microprofile.config.Config;
import org.bimrocket.service.security.store.SecurityDaoConnection;
import org.bimrocket.util.ExpiringCache;
import static org.bimrocket.service.security.SecurityConstants.*;
import static org.bimrocket.util.TextUtils.getISODate;
import static java.lang.Boolean.FALSE;
import java.lang.reflect.Field;
import org.bimrocket.dao.expression.Expression;
import org.bimrocket.dao.expression.OrderByExpression;
import org.bimrocket.dao.expression.io.log.LogExpressionPrinter;
import org.bimrocket.util.EntityDefinition;

import javax.crypto.SecretKey;

/**
 *
 * @author realor
 */
@ApplicationScoped
public class SecurityService
{
  static final Logger LOGGER =
    Logger.getLogger(SecurityService.class.getName());

  static final String BASE = "services.security.";
  static final String USER_REQUEST_ATTRIBUTE = "_user";

  public static final Map<String, Field> userFieldMap =
    EntityDefinition.getInstance(User.class).getFieldMap();

  public static final Map<String, Field> roleFieldMap =
    EntityDefinition.getInstance(Role.class).getFieldMap();

// Exceptions

  static final String USER_ALREADY_EXISTS =
    "SEC001: User already exists.";
  static final String USER_NOT_FOUND =
    "SEC002: User not found.";
  static final String USER_IS_NOT_ACTIVE =
    "SEC003: User is not active.";
  static final String CAN_NOT_CHANGE_PASSWORD =
    "SEC004: Can not change password.";
  static final String ID_IS_REQUIRED =
    "SEC005: User id is required.";
  static final String INVALID_PASSWORD_FORMAT =
    "SEC006: Invalid password format.";
  static final String PASSWORD_IS_REQUIRED =
    "SEC007: Password is required.";

  @Inject
  Instance<HttpServletRequest> requestInstance;

  @Inject
  Config config;

  SecurityDaoStore daoStore;
  LdapConnector ldapConnector;
  JWTUtils jwtUtils;

  String adminPassword;

  ExpiringCache<String> authorizationCache;
  ExpiringCache<User> userCache;
  ExpiringCache<Role> roleCache;
  ConcurrentHashMap<Thread, String> userIdByThread;

  long authorizationCacheTimeout; // seconds
  long userCacheTimeout; // seconds
  long roleCacheTimeout; // seconds

  User anonymousUser;

  boolean ldapEnabled;

  String secretKey;
  long hoursExpirationCookie;

  @PostConstruct
  public void init()
  {
    LOGGER.log(Level.INFO, "Init SecurityService");

    ldapEnabled = config.getValue(BASE + "ldap.enabled", Boolean.class);
    secretKey = config.getValue(BASE + "jwtSecret", String.class);
    hoursExpirationCookie = config.getValue(BASE + "hoursExpirationCookie", Long.class);

    jwtUtils = new JWTUtils(secretKey, hoursExpirationCookie);

    CDI<Object> cdi = CDI.current();

    if (ldapEnabled)
    {
      ldapConnector = cdi.select(LdapConnector.class).get();
      LOGGER.log(Level.INFO, "LDAP enabled: {0}", ldapConnector.getLdapUrl());
    }

    try
    {
      @SuppressWarnings("unchecked")
      Class<SecurityDaoStore> storeClass =
        config.getValue(BASE + "store.class", Class.class);
      daoStore = cdi.select(storeClass).get();
    }
    catch (Exception ex)
    {
      LOGGER.log(Level.SEVERE, "Error initializing SecurityDaoStore [{0}]: {1}",
        new Object[] {
          config.getOptionalValue(BASE + "store.class", String.class).orElse(null),
          ex.toString()
        });
      daoStore = new SecurityEmptyDaoStore();
    }

    LOGGER.log(Level.INFO, "SecurityDaoStore: {0}", daoStore.getClass());

    adminPassword = config.getValue(BASE + "adminPassword", String.class);

    authorizationCacheTimeout = config.getValue(BASE + "authorizationCacheTimeout", Long.class);
    authorizationCache = new ExpiringCache<>(authorizationCacheTimeout * 1000);
    LOGGER.log(Level.INFO, "authorizationCacheTimeout: {0}", authorizationCacheTimeout);

    userCacheTimeout = config.getValue(BASE + "userCacheTimeout", Long.class);
    userCache = new ExpiringCache<>(userCacheTimeout * 1000);
    LOGGER.log(Level.INFO, "userCacheTimeout: {0}", userCacheTimeout);

    roleCacheTimeout = config.getValue(BASE + "roleCacheTimeout", Long.class);
    roleCache = new ExpiringCache<>(roleCacheTimeout * 1000);
    LOGGER.log(Level.INFO, "roleCacheTimeout: {0}", roleCacheTimeout);

    userIdByThread = new ConcurrentHashMap<>();

    anonymousUser = new User();
    anonymousUser.setId(ANONYMOUS_USER);
    anonymousUser.setName(ANONYMOUS_USER);
    anonymousUser.getRoleIds().add(EVERYONE_ROLE);
  }

  @PreDestroy
  public void destroy()
  {
    LOGGER.log(Level.INFO, "Destroying SecurityService");
    daoStore.close();
  }

  public List<User> getUsers(Expression filter, List<OrderByExpression> orderBy)
  {
    LOGGER.log(Level.FINE, "filter: {0}", LogExpressionPrinter.toString(filter));

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      return userDao.find(filter, orderBy);
    }
  }

  public User getUser(String userId)
  {
    LOGGER.log(Level.FINE, "userId: {0}", userId);

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      return userDao.findById(userId);
    }
  }

  public User createUser(User user)
  {
    LOGGER.log(Level.FINE, "userId: {0}", user.getId());

    //Send true to parameter isNewUser
    validateUser(user, true);

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      User prevUser = userDao.findById(user.getId());
      if (prevUser != null)
        throw new InvalidRequestException(USER_ALREADY_EXISTS);

      userCache.remove(user.getId()); // User may exist in cache

      String dateString = getISODate();
      user.setCreationDate(dateString);
      user.setModifyDate(dateString);
      if (user.getActive() == null)
      {
        user.setActive(true);
      }
      return userDao.insert(user);
    }
  }

  public User updateUser(User userUpdate)
  {
    String userId = userUpdate.getId();
    LOGGER.log(Level.FINE, "userId: {0}", userId);

    //Send false to parameter isNewUser
    validateUser(userUpdate, false);

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      User user = userDao.findById(userUpdate.getId());
      if (user == null) throw new NotFoundException(USER_NOT_FOUND);

      userCache.remove(userId);

      user.setName(userUpdate.getName());
      user.setEmail(userUpdate.getEmail());
      if (userUpdate.getActive() != null)
      {
        user.setActive(userUpdate.getActive());
      }
      user.setRoleIds(userUpdate.getRoleIds());
      if (!StringUtils.isBlank(userUpdate.getPasswordHash()))
      {
        user.setPasswordHash(userUpdate.getPasswordHash());
      }
      String dateString = getISODate();
      user.setModifyDate(dateString);
      user = userDao.update(user);
      return user;
    }
  }

  public boolean deleteUser(String userId)
  {
    LOGGER.log(Level.FINE, "userId: {0}", userId);
    userCache.remove(userId);

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getUserDao();
      return userDao.deleteById(userId);
    }
  }

  public List<Role> getRoles(Expression filter, List<OrderByExpression> orderBy)
  {
    LOGGER.log(Level.FINE, "filter: {0}", LogExpressionPrinter.toString(filter));

    try (var conn = daoStore.getConnection())
    {
      var userDao = conn.getRoleDao();
      return userDao.find(filter, orderBy);
    }
  }

  public Role getRole(String roleId)
  {
    LOGGER.log(Level.FINE, "roleId: {0}", roleId);

    try (var conn = daoStore.getConnection())
    {
      var roleDao = conn.getRoleDao();
      return roleDao.findById(roleId);
    }
  }

  public Role createRole(Role role)
  {
    LOGGER.log(Level.FINE, "roleId: {0}", role.getId());

    try (var conn = daoStore.getConnection())
    {
      var roleDao = conn.getRoleDao();
      return roleDao.insert(role);
    }
  }

  public Role updateRole(Role role)
  {
    LOGGER.log(Level.FINE, "roleId: {0}", role.getId());
    roleCache.remove(role.getId());

    try (var conn = daoStore.getConnection())
    {
      var roleDao = conn.getRoleDao();
      return roleDao.update(role);
    }
  }

  public boolean deleteRole(String roleId)
  {
    LOGGER.log(Level.FINE, "roleId: {0}", roleId);
    roleCache.remove(roleId);

    try (var conn = daoStore.getConnection())
    {
      var roleDao = conn.getRoleDao();
      return roleDao.deleteById(roleId);
    }
  }

  public void changePassword(String userId,
    String oldPassword, String newPassword)
  {
    LOGGER.log(Level.FINE, "userId: {0}", userId);

    if (ADMIN_USER.equals(userId) ||
        ANONYMOUS_USER.equals(userId) ||
        StringUtils.isBlank(newPassword))
      throw new InvalidRequestException(CAN_NOT_CHANGE_PASSWORD);

    try (SecurityDaoConnection conn = daoStore.getConnection())
    {
      Dao<User, String> userDao = conn.getUserDao();
      User user = userDao.findById(userId);

      if (user == null ||
          !Objects.equals(hash(oldPassword), user.getPasswordHash()))
        throw new InvalidRequestException(CAN_NOT_CHANGE_PASSWORD);

      checkPasswordFormat(newPassword);
      user.setPasswordHash(hash(newPassword));

      userDao.update(user);
    }
  }

  public void setCurrentUserId(String userId)
  {
    Thread currentThread = Thread.currentThread();
    if (userId == null)
    {
      userIdByThread.remove(currentThread);
    }
    else
    {
      userIdByThread.put(currentThread, userId);
    }
  }

  public String getCurrentUserId()
  {
    User user = getCurrentUser();
    return user.getId();
  }

  public User getCurrentUser()
  {
    User user;
    HttpServletRequest request;
    Cookie cookieAuth = null;
    Claims claims = null;

    // get User from thread map
    String userId = userIdByThread.get(Thread.currentThread());
    if (userId != null)
    {
      user = userCache.get(userId);
      if (user != null) return user;

      user = getUser(userId);
      addUserRoles(user);
      userCache.put(userId, user);

      return user;
    }

    // get User from http request
    try
    {
      request = requestInstance.get();

      user = (User)request.getAttribute(USER_REQUEST_ATTRIBUTE);
      if (user != null) return user;

      Cookie[] cookies = request.getCookies();

      if (cookies == null)
      {
        request.setAttribute(USER_REQUEST_ATTRIBUTE, anonymousUser);
        return anonymousUser;
      }

      for (Cookie c : cookies){
        if (c.getName().equals("auth_token"))
        {
          cookieAuth = c;
          break;
        }
      }

      if (cookieAuth ==null)
      {
        request.setAttribute(USER_REQUEST_ATTRIBUTE, anonymousUser);
        return anonymousUser;
      }
      claims = jwtUtils.verifyToken(cookieAuth.getValue());
      if (claims == null) return anonymousUser;

    }
    catch (Exception ex) // not in servlet context
    {
      return anonymousUser;
    }

    userId = claims.get("userid", String.class);
    if (userId != null)
    {
      user = userCache.get(userId);
      if (user != null) return user;
    }

    user = getUserFromCookie(claims);
    userId = user.getId().trim();

    if (ANONYMOUS_USER.equals(userId)) return anonymousUser;

    addUserRoles(user);

    userCache.put(userId, user);
    request.setAttribute(USER_REQUEST_ATTRIBUTE, user);

    LOGGER.log(Level.FINE, "User {0} identified with roles {1}",
      new Object[] { userId, user.getRoleIds() });

    return user;
  }

  /* private methods */

  private User getUserFromCookie(Claims claims)
  {

    Date expiration = claims.getExpiration();

    if (expiration.before(new Date()))
      return anonymousUser;

    String userId = claims.get("userid", String.class);

    User user = getUser(userId); // get from store
    if (user == null)
    {
      user = new User();
      user.setId(userId);
      user.setName(userId);
    }

    if (FALSE.equals(user.getActive()))
      throw new NotAuthorizedException(USER_IS_NOT_ACTIVE);

    return user;
  }

  private void addUserRoles(User user)
  {
    String userId = user.getId();
    explodeRoles(user.getRoleIds()); // explodeRoles
    user.getRoleIds().add(userId); // add nominal role;
    user.getRoleIds().add(EVERYONE_ROLE);
    user.getRoleIds().add(AUTHENTICATED_ROLE);
    if (ADMIN_USER.equals(userId))
    {
      user.getRoleIds().add(ADMIN_ROLE);
    }
  }

  private void validateUser(User user, boolean isNewUser)
  {
    if (StringUtils.isBlank(user.getId()))
      throw new InvalidRequestException(ID_IS_REQUIRED);

    String password = user.getPassword();
    if (!ldapEnabled && StringUtils.isBlank(password) && isNewUser)
    {
      throw new InvalidRequestException(PASSWORD_IS_REQUIRED);
    }

    if (!StringUtils.isBlank(password))
    {
      checkPasswordFormat(password);
      user.setPasswordHash(hash(password));
      user.setPassword(null);
    }
  }

  private void checkPasswordFormat(String password)
  {
    String passwordPattern =
      config.getValue(BASE + "passwordPattern", String.class);

    if (!password.matches(passwordPattern))
      throw new InvalidRequestException(INVALID_PASSWORD_FORMAT);
  }

  private String hash(String password)
  {
    if (StringUtils.isBlank(password)) return null;

    try
    {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(bytes);
    }
    catch (NoSuchAlgorithmException ex)
    {
      throw new RuntimeException(ex);
    }
  }

  private void explodeRoles(Set<String> roleIds)
  {
    Stack<String> stack = new Stack<>();
    stack.addAll(roleIds);
    while (!stack.isEmpty())
    {
      String roleId = stack.pop();
      Role role = roleCache.get(roleId);
      if (role == null)
      {
        role = getRole(roleId);
        if (role == null)
        {
          // put non peristent Role in cache
          role = new Role();
          role.setId(roleId);
        }
        roleCache.put(roleId, role);
      }

      for (String subRoleId : role.getRoleIds())
      {
        if (!roleIds.contains(subRoleId))
        {
          stack.add(subRoleId);
          roleIds.add(subRoleId);
        }
      }
    }
  }

  /* public methods */

  public boolean checkPasswordHash(User user, String password)
  {
    boolean validPassword = true;
    String passwordHash = hash(password);

    if (!user.getPasswordHash().equals(passwordHash)) validPassword = false;

    return validPassword;
  }

  public NewCookie createHttpOnlyCookie(HttpServletRequest request, String userId)
  {
    String token = createJWTToken(userId);

    boolean isSecureEnv = request.isSecure();
    int secondsExpiration = Math.toIntExact(hoursExpirationCookie) * 60 * 60;

    NewCookie cookie = new NewCookie.Builder("auth_token")
            .value(token)
            .path("/")
            .maxAge(secondsExpiration)
            .secure(true)
            .httpOnly(true)
            .sameSite(NewCookie.SameSite.NONE)
            .build();

    return cookie;
  }

  public String createJWTToken(String userId)
  {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userid", userId);

    return jwtUtils.generateToken(claims);
  }

  public User validateCredentialsLogin(String userId, String password)
  {
    User user = getUser(userId); // get from store
    if (user == null)
    {
      user = new User();
      user.setId(userId);
      user.setName(userId);
    }

    if (FALSE.equals(user.getActive()))
      throw new NotAuthorizedException(USER_IS_NOT_ACTIVE);

    if (ADMIN_USER.equals(userId)) // admin user
    {
      if (!adminPassword.equals(password))
        throw new NotAuthorizedException();
    }
    else if (user.getPasswordHash() == null) // LDAP User
    {
      if (ldapConnector == null ||
              !ldapConnector.validateCredentials(userId, password))
        throw new NotAuthorizedException();
    }
    else // check hashed password in User
    {
      String passwordHash = hash(password);

      if (!user.getPasswordHash().equals(passwordHash))
        throw new NotAuthorizedException();
    }
    return user;
  }

  public NewCookie destroyHttpOnlyCookie(HttpServletRequest request)
  {
    boolean isSecureEnv = request.isSecure();

    NewCookie cookie = new NewCookie.Builder("auth_token")
            .value("")
            .path("/")
            .maxAge(0)
            .secure(isSecureEnv)
            .httpOnly(true)
            .build();

    return cookie;
  }
}
