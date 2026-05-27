package it.univaq.sose.daas;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/docs")
public class SwaggerDocsResource {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response docs() {
        String html = """
                <!DOCTYPE html>
                <html lang=\"en\">
                  <head>
                    <meta charset=\"UTF-8\" />
                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                    <title>DaaS API Docs</title>
                    <link rel=\"stylesheet\" type=\"text/css\" href=\"/swagger-ui.css\" />
                    <style>
                      html, body { margin: 0; padding: 0; }
                      #swagger-ui { min-height: 100vh; }
                    </style>
                  </head>
                  <body>
                    <div id=\"swagger-ui\"></div>
                    <script src=\"/swagger-ui-bundle.js\"></script>
                    <script src=\"/swagger-ui-standalone-preset.js\"></script>
                    <script>
                      window.ui = SwaggerUIBundle({
                        url: '/openapi.json',
                        dom_id: '#swagger-ui',
                        deepLinking: true,
                        presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                        plugins: [SwaggerUIBundle.plugins.DownloadUrl],
                        layout: 'StandaloneLayout'
                      });
                    </script>
                  </body>
                </html>
                """;

        return Response.ok(html).build();
    }
}
