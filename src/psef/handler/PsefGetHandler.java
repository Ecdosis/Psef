/*
 * This file is part of Psef.
 *
 *  Psef is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Psef is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Psef.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2014
 */
package psef.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import psef.exception.PsefException;
import calliope.core.Utils;
import psef.constants.Service;
import calliope.core.handler.GetHandler;

/**
 * Handle a GET request for various image types, text, GeoJSON
 * @author desmond
 */
public class PsefGetHandler extends GetHandler {
    
    /**
     * Handle a request for one of the data types supported here
     * @param request the http request
     * @param response the http response
     * @param urn the remainder of the request urn to be processed
     * @throws PagesException 
     */
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws PsefException {
        try {
            String service = Utils.first(urn);
            if ( service.equals(Service.PROJECT) )
                new ProjectGetHandler().handle(request,response,"");
            throw new Exception("Unknown service "+service);
        } catch (Exception e) {
            throw new PsefException(e);
        }
    }
}