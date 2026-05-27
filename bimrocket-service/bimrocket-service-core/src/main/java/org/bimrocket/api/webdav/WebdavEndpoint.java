/*
 * BIMROCKET
 *
 * Copyright (C) 2021-2026, Ajuntament de Sant Feliu de Llobregat
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
package org.bimrocket.api.webdav;

import org.bimrocket.rest.methods.UNLOCK;
import org.bimrocket.rest.methods.LOCK;
import org.bimrocket.rest.methods.MKCOL;
import org.bimrocket.rest.methods.COPY;
import org.bimrocket.rest.methods.ACL;
import org.bimrocket.rest.methods.PROPFIND;
import org.bimrocket.rest.methods.MOVE;
import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.bimrocket.api.ApiResult.OK;
import org.bimrocket.api.security.User;
import org.bimrocket.service.file.FileService;
import org.bimrocket.service.file.FindOptions;
import org.bimrocket.service.file.Metadata;
import org.bimrocket.service.file.util.MutableACL;
import org.bimrocket.service.security.SecurityService;
import org.bimrocket.util.URIEncoder;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author realor
 */
@Path("cloudfs")
@RequestScoped
@Produces("*/*")
@Tag(name="Webdav", description="Webdav service")
public class WebdavEndpoint
{
  static final String WEBDAV_METHODS =
    "HEAD,GET,POST,PUT,DELETE,OPTIONS,PROPFIND,PROPPATCH,MKCOL,ACL,LOCK,UNLOCK,COPY,MOVE";

  @Inject
  FileService fileService;

  @Inject
  SecurityService securityService;

  @OPTIONS
  @Path("/")
  @PermitAll
  public Response optionsRoot()
  {
    return options();
  }

  @OPTIONS
  @Path("{path: .*}")
  @PermitAll
  public Response options()
  {
    return Response.ok()
      .header("DAV", "1,2")
      .header("Allow", WEBDAV_METHODS)
      .header("MS-Author-Via", "DAV")
      .build();
  }

  @HEAD
  @Path("/")
  @PermitAll
  public Response headRoot(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
  {
    return head(uriInfo, headers);
  }

  @HEAD
  @Path("{path: .*}")
  @PermitAll
  public Response head(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
  {
    if (true) Response.ok(OK).build();

    org.bimrocket.service.file.Path path = getPath(uriInfo);

    Metadata metadata = fileService.get(path);

    return buildMetadataResponse(metadata, headers).build();
  }

  @GET
  @Path("/")
  @PermitAll
  @Produces("application/json")
  public Response getRoot(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
    throws IOException
  {
    return get(uriInfo, headers);
  }

  @GET
  @Path("{path: .*}")
  @PermitAll
  @Produces("application/json")
  public Response get(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
    throws IOException
  {
    org.bimrocket.service.file.Path path = getPath(uriInfo);

    Metadata metadata = fileService.get(path);

    if (metadata.isCollection())
    {
      return propfind(uriInfo, headers.getHeaderString("Depth"), null);
    }

    InputStream is = fileService.read(path);

    return buildMetadataResponse(metadata, headers)
      .entity(is)
      .build();
  }

  @PUT
  @Path("/")
  @PermitAll
  public Response putRoot(
    @Context UriInfo uriInfo,
    InputStream body,
    @HeaderParam("Content-Type") String contentType)
    throws IOException
  {
    return put(uriInfo, body, contentType);
  }

  @PUT
  @Path("{path: .*}")
  @PermitAll
  public Response put(
    @Context UriInfo uriInfo,
    InputStream body,
    @HeaderParam("Content-Type") String contentType)
    throws IOException
  {
    org.bimrocket.service.file.Path path = getPath(uriInfo);

    System.out.println("Path: " + path);

    try (body)
    {
      fileService.write(path, body, contentType);
    }

    return Response.ok().build();
  }

  @DELETE
  @Path("{path: .*}")
  @PermitAll
  public Response delete(@Context UriInfo uriInfo)
    throws IOException
  {
    org.bimrocket.service.file.Path path = getPath(uriInfo);

    fileService.delete(path);

    return Response.ok().build();
  }

  @MKCOL
  @Path("/")
  @PermitAll
  public Response mkcolRoot(@Context UriInfo uriInfo)
  {
    return mkcol(uriInfo);
  }

  @MKCOL
  @Path("{path: .*}")
  @PermitAll
  public Response mkcol(@Context UriInfo uriInfo)
  {
    org.bimrocket.service.file.Path path = getPath(uriInfo);

    fileService.makeCollection(path);

    return Response.status(201).build();
  }

  @PROPFIND
  @Path("/")
  @PermitAll
  public Response propfindRoot(
    @Context UriInfo uriInfo,
    @HeaderParam("Depth") String depth,
    String body)
  {
    return propfind(uriInfo, depth, body);
  }

  @PROPFIND
  @Path("{path: .*}")
  @PermitAll
  public Response propfind(
    @Context UriInfo uriInfo,
    @HeaderParam("Depth") String depth,
    String body)
  {
    org.bimrocket.service.file.Path path = getPath(uriInfo);

    Set<String> requested = parsePropfindBody(body);

    if (requested.contains("{DAV:}acl"))
    {
      org.bimrocket.service.file.ACL acl = fileService.getACL(path);
      if (acl == null) acl = new MutableACL();

      String xml = ACLXMLSerializer.serialize(acl);

      return Response.status(207)
          .type("application/xml")
          .entity(xml)
          .build();
    }

    FindOptions options = new FindOptions();

    if (depth != null)
    {
      depth = depth.toLowerCase();
      options.setIncludeRoot(!depth.contains("noroot"));
      if (depth.contains("infinity")) options.setDepth(FindOptions.INFINITY);
      else if (depth.contains("0")) options.setDepth(0);
    }

    List<Metadata> metas = fileService.find(path, options);

    if (metas.isEmpty())
    {
      return Response.status(404)
        .type("text/plain")
        .entity("Not found")
        .build();
    }

    String xml = buildMultistatus(metas, uriInfo);

    return Response.status(207)
      .type("text/xml")
      .header("DAV", "1,2")
      .entity(xml)
      .build();
  }

  @COPY
  @Path("{path: .*}")
  @PermitAll
  public Response copy(
    @Context UriInfo uriInfo,
    @HeaderParam("Destination") String destination)
    throws IOException
  {
    org.bimrocket.service.file.Path source = getPath(uriInfo);
    org.bimrocket.service.file.Path dest = getDestinationPath(destination, uriInfo);

    fileService.copy(source, dest);

    return Response.status(201).build();
  }

  @MOVE
  @Path("{path: .*}")
  @PermitAll
  public Response move(
    @Context UriInfo uriInfo,
    @HeaderParam("Destination") String destination)
    throws IOException
  {
    org.bimrocket.service.file.Path source = getPath(uriInfo);
    org.bimrocket.service.file.Path dest = getDestinationPath(destination, uriInfo);

    System.out.println("MOVE: " +  source + "->" +  dest);

    fileService.move(source, dest);

    return Response.status(201).build();
  }

  @LOCK
  @Path("{path: .*}")
  @PermitAll
  public Response lock(@Context UriInfo uriInfo)
  {
    fileService.lock(getPath(uriInfo));
    return Response.ok().build();
  }

  @UNLOCK
  @Path("{path: .*}")
  @PermitAll
  public Response unlock(@Context UriInfo uriInfo)
  {
    fileService.unlock(getPath(uriInfo));
    return Response.ok().build();
  }

  @ACL
  @Path("/")
  @PermitAll
  public Response aclRoot(
    @Context UriInfo uriInfo,
    String body)
    throws IOException
  {
    return acl(uriInfo, body);
  }

  @ACL
  @Path("{path: .*}")
  @PermitAll
  public Response acl(
    @Context UriInfo uriInfo,
    String body)
    throws IOException
  {
    org.bimrocket.service.file.Path path = getPath(uriInfo);

    User user = securityService.getCurrentUser();

    org.bimrocket.service.file.ACL acl =
      ACLXMLDeserializer.deserialize(body, user.getId());

    fileService.setACL(path, acl);

    return Response.ok().build();
  }

  // private methods

  private ResponseBuilder buildMetadataResponse(
    Metadata metadata, HttpHeaders headers)
  {
    Response.ResponseBuilder rb = Response.ok();

    if (metadata.isCollection())
    {
      return rb;
    }

    rb.type(metadata.getContentType());
    rb.header("Last-Modified", new Date(metadata.getLastModifiedDate()));
    rb.header("Cache-Control", "no-cache");

    String etag = metadata.getEtag();
    if (etag == null)
    {
      etag = "W/\"%s-%s" + metadata.getContentLength() + "-" +
        metadata.getLastModifiedDate() + "\"";
    }

    String ifNoneMatch = headers.getHeaderString("If-None-Match");
    if (etag != null && etag.equals(ifNoneMatch))
    {
      return Response.notModified().tag(etag);
    }

    rb.tag(etag);
    rb.header("Content-Length", metadata.getContentLength());

    return rb;
  }

  private Set<String> parsePropfindBody(String body)
  {
    if (body == null || body.isBlank())
    {
      return Collections.emptySet();
    }

    try
    {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);

      DocumentBuilder builder = factory.newDocumentBuilder();
      org.w3c.dom.Document doc = builder.parse(
        new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8))
      );

      NodeList propNodes = doc.getElementsByTagNameNS("DAV:", "prop");
      if (propNodes.getLength() == 0)
      {
        return Collections.emptySet();
      }

      Set<String> props = new HashSet<>();

      Node prop = propNodes.item(0);
      NodeList children = prop.getChildNodes();

      for (int i = 0; i < children.getLength(); i++)
      {
        Node n = children.item(i);

        if (n.getNodeType() == Node.ELEMENT_NODE)
        {
          String ns = n.getNamespaceURI();
          String local = n.getLocalName();
          props.add("{" + ns + "}" + local);
        }
      }
      return props;
    }
    catch (Exception e)
    {
      throw new RuntimeException("Invalid PROPFIND XML", e);
    }
  }

  private String buildMultistatus(List<Metadata> metas, UriInfo uriInfo)
  {
    StringBuilder sb = new StringBuilder();

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));

    sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
    sb.append("<D:multistatus xmlns:D=\"DAV:\">");

    String servicePath = getServicePathName(uriInfo);

    for (Metadata m : metas)
    {
      String href = URIEncoder.encode(servicePath + m.getPath().toString());

      sb.append("<D:response>");
      sb.append("<D:href>").append(href).append("</D:href>");
      sb.append("<D:propstat>");
      sb.append("<D:prop>");

      sb.append("<D:creationdate>")
        .append(df.format(new Date(m.getCreationDate())))
        .append("</D:creationdate>");

      sb.append("<D:getlastmodified>")
        .append(df.format(new Date(m.getLastModifiedDate())))
        .append("</D:getlastmodified>");

      if (m.isCollection())
      {
        sb.append("<D:resourcetype><D:collection/></D:resourcetype>");
      }
      else
      {
        sb.append("<D:getcontentlength>")
          .append(m.getContentLength())
          .append("</D:getcontentlength>");
        sb.append("<D:resourcetype/>");
      }

      sb.append("<D:supportedlock>")
        .append("<D:lockentry>")
        .append("<D:lockscope><D:exclusive/></D:lockscope>")
        .append("<D:locktype><D:write/></D:locktype>")
        .append("</D:lockentry>")
        .append("<D:lockentry>")
        .append("<D:lockscope><D:shared/></D:lockscope>")
        .append("<D:locktype><D:write/></D:locktype>")
        .append("</D:lockentry>")
        .append("</D:supportedlock>");

      sb.append("</D:prop>");
      sb.append("<D:status>HTTP/1.1 200 OK</D:status>");
      sb.append("</D:propstat>");
      sb.append("</D:response>");
    }

    sb.append("</D:multistatus>");

    return sb.toString();
  }

  private org.bimrocket.service.file.Path getDestinationPath(
    String destination, UriInfo uriInfo)
  {
    URI uri = URI.create(destination);

    String servicePath = getServicePathName(uriInfo);
    String path = uri.getPath().substring(servicePath.length());

    return new org.bimrocket.service.file.Path(path);
  }

  private org.bimrocket.service.file.Path getPath(UriInfo uriInfo)
  {
    String serviceName = uriInfo.getPathSegments().get(0).getPath();
    String path = uriInfo.getPath().substring(serviceName.length() + 1);
    return new org.bimrocket.service.file.Path(path);
  }

  private String getServicePathName(UriInfo uriInfo)
  {
    String serviceName = uriInfo.getPathSegments().get(0).getPath();
    return uriInfo.getBaseUri().getPath() + serviceName;
  }
}
