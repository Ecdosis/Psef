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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URL;
import psef.exception.PsefException;
/**
 * 
 */
public class HTMLFilter 
{
    String src;
    String base;
    String host;
    HTMLFilter( String src, String base, String host )
    {
        this.src = src;
        this.base = base;
        this.host = host;
    }
    public String filter() throws PsefException
    {
        Document doc = Jsoup.parse(src);
        Elements scripts = doc.getElementsByTag("script");
        for (Element script : scripts) 
        {
            String scriptSrc = script.attr("src");
            try
            {
                if ( scriptSrc != null && scriptSrc.length()>0 )
                {
                    if ( !scriptSrc.startsWith("http") )
                        scriptSrc = "http://"+host+base+"/"+scriptSrc;
                    URL url =  url = new URL(scriptSrc);
                    System.out.println(url.getPath());
                }
            }
            catch ( Exception e )
            {
                throw new PsefException(e);
            }
        }
        return src;
    }
}
