/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.fit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.olingo.commons.api.edm.constants.ODataServiceVersion;
import org.apache.olingo.fit.metadata.Metadata;
import org.apache.olingo.fit.utils.Accept;
import org.apache.olingo.fit.utils.ConstantKey;
import org.apache.olingo.fit.utils.Constants;
import org.apache.olingo.fit.utils.FSManager;
import org.springframework.stereotype.Service;

@Service
@Path("/V30/OpenType.svc")
public class V3OpenType {

  private static final Pattern GUID = Pattern.compile("guid'(.*)'");

  private final V3Services services;

  private final Metadata openMetadata;

  public V3OpenType() throws Exception {
    this.openMetadata = new Metadata(FSManager.instance(ODataServiceVersion.V30).
            readFile("openType" + StringUtils.capitalize(Constants.get(ODataServiceVersion.V30, ConstantKey.METADATA)),
                    Accept.XML));
    this.services = new V3Services() {

      @Override
      protected Metadata getMetadataObj() {
        return openMetadata;
      }
    };
  }

  private Response replaceServiceName(final Response response) {
    try {
      final String content = IOUtils.toString((InputStream) response.getEntity(), "UTF-8").
              replaceAll("Static\\.svc", "OpenType.svc");

      final Response.ResponseBuilder builder = Response.status(response.getStatus());
      for (String headerName : response.getHeaders().keySet()) {
        for (Object headerValue : response.getHeaders().get(headerName)) {
          builder.header(headerName, headerValue);
        }
      }

      final InputStream toBeStreamedBack = IOUtils.toInputStream(content, "UTF-8");
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      IOUtils.copy(toBeStreamedBack, baos);
      IOUtils.closeQuietly(toBeStreamedBack);

      builder.header("Content-Length", baos.size());
      builder.entity(new ByteArrayInputStream(baos.toByteArray()));

      return builder.build();
    } catch (Exception e) {
      return response;
    }
  }

  /**
   * Provide sample large metadata.
   *
   * @return metadata.
   */
  @GET
  @Path("/$metadata")
  @Produces(MediaType.APPLICATION_XML)
  public Response getMetadata() {
    return services.getMetadata("openType" + StringUtils.capitalize(
            Constants.get(ODataServiceVersion.V30, ConstantKey.METADATA)));
  }

  @GET
  @Path("/{entitySetName}({entityId})")
  public Response getEntity(
          @HeaderParam("Accept") @DefaultValue(StringUtils.EMPTY) String accept,
          @PathParam("entitySetName") String entitySetName,
          @PathParam("entityId") String entityId,
          @QueryParam("$format") @DefaultValue(StringUtils.EMPTY) String format,
          @QueryParam("$expand") @DefaultValue(StringUtils.EMPTY) String expand,
          @QueryParam("$select") @DefaultValue(StringUtils.EMPTY) String select) {

    final Matcher matcher = GUID.matcher(entityId);
    return replaceServiceName(services.getEntityInternal(accept, entitySetName,
            matcher.matches() ? matcher.group(1) : entityId, format, expand, select, false));
  }

  @POST
  @Path("/{entitySetName}")
  @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON})
  @Consumes({MediaType.APPLICATION_ATOM_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
  public Response postNewEntity(
          @HeaderParam("Accept") @DefaultValue(StringUtils.EMPTY) String accept,
          @HeaderParam("Content-Type") @DefaultValue(StringUtils.EMPTY) String contentType,
          @HeaderParam("Prefer") @DefaultValue(StringUtils.EMPTY) String prefer,
          @PathParam("entitySetName") final String entitySetName,
          final String entity) {

    return replaceServiceName(services.postNewEntity(accept, contentType, prefer, entitySetName, entity));
  }

  @DELETE
  @Path("/{entitySetName}({entityId})")
  public Response removeEntity(
          @PathParam("entitySetName") String entitySetName,
          @PathParam("entityId") String entityId) {

    final Matcher matcher = GUID.matcher(entityId);
    return replaceServiceName(services.removeEntity(entitySetName,
            matcher.matches() ? matcher.group(1) : entityId));
  }
}
