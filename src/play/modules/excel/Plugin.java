/**
 *
 * Copyright 2010, greenlaw110@gmail.com.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * User: Green Luo
 * Date: Mar 26, 2010
 *
 */
package play.modules.excel;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.codec.net.URLCodec;

import play.PlayPlugin;
import play.exceptions.UnexpectedException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.RenderArgs;
import play.mvc.results.Result;
import play.templates.Template;
import play.vfs.VirtualFile;


public class Plugin extends PlayPlugin {
    
    public static final String VERSION = "1.2.3";

    public static PlayPlugin templateLoader = null;
    
    private final static Pattern p_ = Pattern.compile(".*\\.(xls|xlsx)");
    @Override
    public Template loadTemplate(VirtualFile file) {
        if (!p_.matcher(file.getName()).matches()) return null;
        if (null == templateLoader) return new ExcelTemplate(file);
        return templateLoader.loadTemplate(file);
    }
    
    /*
     * Set response header if needed
     */
    private static final URLCodec encoder = new URLCodec();
    @Override
    public void onActionInvocationResult(Result result) {
        Request request = Request.current();
        if (null == request.format || !request.format.matches("(csv|xls|xlsx)"))
            return;

        Response response = Response.current();
        RenderArgs renderArgs = RenderArgs.current();
        if (!response.headers.containsKey("Content-Disposition")) {
            String fileName = renderArgs.get(RenderExcel.RA_FILENAME,
                    String.class);
            if (fileName == null) {
                response.setHeader("Content-Disposition",
                        "attachment; filename=export." + request.format);
            } else {
                try {
                    response.setHeader(
                            "Content-Disposition",
                            "attachment; filename="
                                    + encoder.encode(fileName, "utf-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new UnexpectedException(e);
                }
            }

            if ("xls".equals(request.format)) {
                response.setContentTypeIfNotSet("application/vnd.ms-excel");
            } else if ("xlsx".equals(request.format)) {
                response.setContentTypeIfNotSet("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            } else if ("csv".equals(request.format)) {
                response.setContentTypeIfNotSet("text/csv");
            }
        }
    }
    
    public static class ExcelTemplate extends Template {
        
        private File file = null;
        private RenderExcel r_ = null;
        
        public ExcelTemplate(VirtualFile file) {
            this.name = file.relativePath();
            this.file = file.getRealFile();
        }
        
        public ExcelTemplate(RenderExcel render) {
            r_ = render;
        }

        @Override
        public void compile() {
            if (!file.canRead()) throw new UnexpectedException("template file not readable: " + name);
        }

        @Override
        protected String internalRender(Map<String, Object> args) {
            throw null == r_ ? new RenderExcel(name, args) : r_;
        }
    }

}
