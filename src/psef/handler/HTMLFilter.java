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
import java.net.URLConnection;
import java.io.InputStream;
import java.io.FileOutputStream;
import calliope.core.Utils;
import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import psef.exception.PsefException;
/**
 * 
 */
public class HTMLFilter 
{
    String src;
    String base;
    String host;
    File root;
    HTMLFilter( File root, String src, String base, String host )
    {
        this.src = src;
        this.base = base;
        this.host = host;
        this.root = root;
    }
    /**
     * Download a remote file and save it locally
     * @param url the url to fetch it from
     * @param path the local path to store it at
     * @throws PsefException 
     */
    void downloadScript (URL url, String path ) throws PsefException
    {
        try
        {
            URLConnection connection = url.openConnection();
            InputStream is = connection.getInputStream();
            byte[] data = Utils.readStream(is);
            File dst = new File( root, path );
            if ( !dst.exists() )
            {
                File parent = dst.getParentFile();
                if ( !parent.exists() )
                    parent.mkdirs();
                dst.createNewFile();
                FileOutputStream out = new FileOutputStream(dst);
                out.write(data);
                out.close();
            }
        }
        catch ( Exception e )
        {
            throw new PsefException(e);
        }
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
                    if ( url.getPath().startsWith(base) )
                    {
                        String newPath = url.getPath().substring(base.length());
                        String newUrl = "scripts"+newPath;
                        script.attr("src",newUrl);
                        downloadScript(url,newUrl);
                    }
                }
            }
            catch ( Exception e )
            {
                throw new PsefException(e);
            }
        }
        // write converted dom to a string
        StringWriter sw = new StringWriter(src.length());
        PrintWriter writer = new PrintWriter(sw);
        writer.write( doc.html() ) ;
        writer.flush();
        writer.close(); 
        return sw.toString();
    }
}
