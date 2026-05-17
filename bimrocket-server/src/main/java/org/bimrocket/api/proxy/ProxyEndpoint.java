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
package org.bimrocket.api.proxy;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.bimrocket.rest.RequestContext;
import org.bimrocket.rest.methods.ACL;
import org.bimrocket.rest.methods.COPY;
import org.bimrocket.rest.methods.MKCOL;
import org.bimrocket.rest.methods.MOVE;
import org.bimrocket.rest.methods.PROPFIND;
import org.bimrocket.service.proxy.ProxyService;

/**
 *
 * @author realor
 */
@Path("proxy")
@Produces("*/*")
@Tag(name="Proxy", description="Proxy service")
@Hidden
public class ProxyEndpoint
{
  @Inject
  ProxyService proxyService;

  @Inject
  RequestContext requestContext;

  @OPTIONS
  @Path("/")
  @PermitAll
  public Response options(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
    throws Exception
  {
    return forwardRequest("OPTIONS", uriInfo, headers, null);
  }

  @HEAD
  @Path("/")
  @PermitAll
  public Response head(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
    throws Exception
  {
    return forwardRequest("HEAD", uriInfo, headers, null);
  }

  @GET
  @Path("/")
  @PermitAll
  public Response get(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
    throws Exception
  {
    return forwardRequest("GET", uriInfo, headers, null);
  }

  @POST
  @Path("/")
  @PermitAll
  public Response post(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers,
    InputStream body)
    throws Exception
  {
    return forwardRequest("POST", uriInfo, headers, body);
  }

  @PUT
  @Path("/")
  @PermitAll
  public Response put(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers,
    InputStream body)
    throws Exception
  {
    return forwardRequest("PUT", uriInfo, headers, body);
  }

  @DELETE
  @Path("/")
  @PermitAll
  public Response delete(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
    throws Exception
  {
    return forwardRequest("DELETE", uriInfo, headers, null);
  }

  @PROPFIND
  @Path("/")
  @PermitAll
  public Response propfind(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers,
    InputStream body)
    throws Exception
  {
    return forwardRequest("PROPFIND", uriInfo, headers, body);
  }

  @MKCOL
  @Path("/")
  @PermitAll
  public Response mkcol(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
    throws Exception
  {
    return forwardRequest("MKCOL", uriInfo, headers, null);
  }

  @COPY
  @Path("/")
  @PermitAll
  public Response copy(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
    throws Exception
  {
    return forwardRequest("COPY", uriInfo, headers, null);
  }

  @MOVE
  @Path("/")
  @PermitAll
  public Response move(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers)
    throws Exception
  {
    return forwardRequest("MOVE", uriInfo, headers, null);
  }

  @ACL
  @Path("/")
  @PermitAll
  public Response acl(
    @Context UriInfo uriInfo,
    @Context HttpHeaders headers,
    InputStream body)
    throws Exception
  {
    return forwardRequest("ACL", uriInfo, headers, body);
  }

  /*** private methods ***/

  private Response forwardRequest(
    String method,
    UriInfo uriInfo,
    HttpHeaders incomingHeaders,
    InputStream body)
    throws Exception
  {
    HttpResponse<InputStream> httpResponse = proxyService.forwardRequest(
      method,
      uriInfo.getQueryParameters(),
      incomingHeaders.getRequestHeaders(),
      requestContext.getRemoteAddress(),
      body);

    return sendResponse(httpResponse);
  }

  private Response sendResponse(HttpResponse<InputStream> httpResponse)
    throws Exception
  {
    Map<String, List<String>> headersMap = httpResponse.headers().map();

    Response.ResponseBuilder rb = Response.ok();

    rb.status(httpResponse.statusCode());
    rb.entity(httpResponse.body());

    for (Map.Entry<String, List<String>> entry : headersMap.entrySet())
    {
      String name = entry.getKey();
      if (name != null
        && !name.equalsIgnoreCase("Transfer-Encoding")
        && !name.toLowerCase().startsWith("access-control"))
      {
        rb.header(name, entry.getValue().get(0));
      }
    }
    return rb.build();
  }
}
