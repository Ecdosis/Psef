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
 *  (c) copyright Desmond Schmidt 2015
 */
package psef.handler;

import java.net.URL;
import java.util.StringTokenizer;
import psef.exception.PsefException;

/**
 * Represent a css type url
 * @author desmond
 */
public class CSSUrl 
{
    int pos;
    String path;
    String localPath;
    /**
     * Read the "url" part after an @import
     * @param text the text to scan
     * @return the first char after the url or 0
     */
    public CSSUrl( String text )
    {
        StringTokenizer st = new StringTokenizer(text,"(); \"\'",true);
        int state = 0;
        boolean atEnd = false;
        StringBuilder sb = new StringBuilder();
        while ( !atEnd && st.hasMoreTokens() )
        {
            String token = st.nextToken();
            pos += token.length();
            switch ( state )
            {
                case 0: // looking for "url"
                    if ( token.equals("url") )
                        state = 1;
                    break;
                case 1: // look for "("
                    if ( token.equals("(") )
                        state = 2;
                    break;
                case 2: // look for quotations or body
                    if ( token.equals("\"") || token.equals("\'") )
                        state = 3;
                    break;
                case 3: // looking for body
                    if ( token.equals("\"")||token.equals("\'") )
                        state = 4;
                    else
                        sb.append(token);
                    break;
                case 4: // looking for ")"
                    if ( token.equals(")") )
                        state = 5;
                    break;
                case 5: // looking for ";"
                    if ( token.equals(";")||token.equals(" ")||token.equals("\n") )
                        atEnd = true;
                    break;
            }
        }
        path = sb.toString();
    }
    /**
     * Get the actual number of chars traversed to read the URL
     * @return an int
     */
    int getPos()
    {
        return pos;
    }
    /**
     * Get the url in order to fetch the original resource (pre revise)
     * @param host the host in case the url is relative
     * @param base the base in case the url is relative
     * @return the full url
     */
    String getUrl( String host, String base )
    {
        if ( path.startsWith("http") )
            return path;
        else
            return "http://"+host+base+path;
    }
    /**
     * Revise the path component
     * @param host the host on which the css file resides
     * @param base the base from which relative addresses start
     * @param folder the folder into which the resource will be stored
     * @return the revised path
     * @throws PsefException 
     */
    void revise( String host, String base, String folder ) throws PsefException
    {
        try
        {
            String fullPath = path;
            if ( !fullPath.startsWith("http") )
                fullPath = "http://"+host+base+"/"+path;
            URL url = new URL(fullPath);
            if ( url.getPath().startsWith(base) )
            {
                String newPath = url.getPath().substring(base.length());
                localPath = folder+newPath;
            }
            else
                localPath = path;
        }
        catch ( Exception e )
        {
            throw new PsefException(e);
        }
    }
    /**
     * Convert back into a CSS url
     * @return a string
     */
    public String getLocalPath()
    {
        return localPath;
    }
    public String toString()
    {
        return "url('"+localPath+"');";
    }
}
