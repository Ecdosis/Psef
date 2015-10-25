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
import org.jsoup.nodes.DataNode;
import java.util.List;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import org.apache.commons.codec.binary.Base64;
/**
 * Create a Web archive from a single web page
 */
public class HTMLFilter 
{
    /** the source HTML text */
    String src;
    /** base path within web-dir of site */
    String base;
    /** host dns name */
    String host;
    /**  relative path from root */
    String relPath;
    /** the root destination directory */
    File root;
    HTMLFilter( File root, String src, String base, String relPath, String host )
    {
        this.src = src;
        this.base = base;
        this.relPath = relPath;
        this.host = host;
        this.root = root;
    }
    /**
     * Download a remote file and save it locally
     * @param url the url to fetch it from
     * @param path the local path to store it at
     * @throws PsefException 
     */
    void downloadResource (URL url, String path ) throws PsefException
    {
        try
        {
            File dst = new File( root, path );
            if ( !dst.exists() )
            {
                URLConnection connection = url.openConnection();
                InputStream is = connection.getInputStream();
                byte[] data = Utils.readStream(is);
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
    /**
     * Convert and download all scripts
     * @param doc the DOM document
     * @throws PsefException 
     */
    private void filterScripts( Document doc ) throws PsefException
    {
        Elements scripts = doc.getElementsByTag("script");
        for (Element script : scripts) 
        {
            String scriptSrc = script.attr("src");
            try
            {
                if ( scriptSrc != null && scriptSrc.length()>0 )
                {
                    if ( !scriptSrc.startsWith("http") )
                        scriptSrc = "http://"+host+base+relPath+"/"+scriptSrc;
                    URL url = new URL(scriptSrc);
                    if ( url.getPath().startsWith(base) )
                    {
                        String newPath = url.getPath().substring(base.length());
                        String newUrl = "scripts"+newPath;
                        script.attr("src",newUrl);
                        downloadResource(url,newUrl);
                    }
                    // else what?
                }
            }
            catch ( Exception e )
            {
                throw new PsefException(e);
            }
        }
    }
    /**
     * See if the string starts with "@import"
     * @param text the text to scan
     * @return the offset of the first character after @import or 0
     */
    int readAtImport( String text )
    {
        if ( text.trim().startsWith("@import") )
            return text.indexOf("@import")+7;
        else
            return 0;
    }
    /**
     * Revise all "style" statements with @import directives
     * @param doc the DOM document
     * @throws PsefException 
     */
    private void filterStyles( Document doc ) throws PsefException
    {
        try
        {
            Elements styles = doc.getElementsByTag("style");
            for (Element style : styles) 
            {
                List<DataNode> data = style.dataNodes();
                String styleText = "";
                StringBuilder sb = new StringBuilder();
                for ( DataNode node : data )
                {
                    styleText = node.getWholeData();
                    int pos = readAtImport(styleText);
                    while ( pos > 0 )
                    {
                        sb.append( "@import ");
                        styleText = styleText.substring(pos);
                        CSSUrl cssu = new CSSUrl( styleText );
                        styleText = styleText.substring(cssu.getPos());
                        cssu.revise(host, base+relPath, "styles" );
                        URL u = new URL(cssu.getUrl(host,base+relPath));
                        downloadResource(u,cssu.getLocalPath());
                        sb.append( cssu.toString() );
                        sb.append("\n");
                        pos = readAtImport(styleText);
                    }
                    node.setWholeData(sb.toString());
               }
               sb.append( styleText );
               //style.( sb.toString() );
            }
        }
        catch ( Exception e )
        {
            throw new PsefException( e );
        }
    }
    /**
     * Convert a relative href to an absolute one
     * @param href a possibly relative href
     * @return the full http:// href
     */
    private String cleanHref( String href )
    {
        if ( href.startsWith("http://") )
            return href;
        else
            return "http://"+host+relPath+href;
    }
    /**
     * Remove just the base element from the full path
     * @param path a full path 
     * @return the path relative to base
     */
    private String cleanPath( String path )
    {
        if ( path.startsWith(base) )
            path = path.substring(base.length());
        else
            path = path;
        if ( path.equals("/") )
            path = "index.html";
        return path;
    }
    /**
     * Convert the link hrefs to their local equivalents
     * @param doc the document
     */
    private void filterLinks( Document doc ) throws PsefException
    {
        try
        {
            Elements links = doc.getElementsByTag("link");
            for (Element link : links) 
            {
                String type = link.attr("type");
                String href = link.attr("href");
                href = cleanHref(href);
                String folder = "other";
                if ( type.equals("text/css") )
                    folder = "styles";
                else if ( type.equals("text/javascript") )
                    folder = "scripts";
                else if ( type.startsWith("image/") )
                    folder = "corpix";
                URL u = new URL( href );
                String localPath = folder+"/"+cleanPath(u.getPath());
                downloadResource(u,localPath);
                link.attr("href",localPath);
            }
        }
        catch ( MalformedURLException e )
        {
            throw new PsefException(e);
        }
    }
    /**
     * Update all the anchors
     * @param doc the DOM document
     */
    private void filterAnchors( Document doc ) throws PsefException
    {
        try
        {
            Elements anchors = doc.getElementsByTag("a");
            for (Element anchor : anchors) 
            {
                String href = anchor.attr("href");
                String localPath = "";
                URL u = null;
                if ( href.startsWith("#") )
                    continue;
                else if ( href.startsWith("http:") )
                {
                    u = new URL(href);
                    if ( u.getHost().equals(host) && u.getPath().startsWith(base) )
                        localPath = cleanPath(u.getPath());
                    u = null;
                }
                else
                    localPath = cleanPath(href);
                if ( localPath.length()>0 && u == null )
                    u = new URL(cleanHref(href));
                if ( u != null && localPath.length()>0 )
                {
                    if ( !localPath.equals("index.html") )
                    {
                        localPath = "anchors"+localPath;
                        String[] parts = localPath.split("\\?");
                        if ( parts.length == 2 )
                        {
                            MessageDigest md = MessageDigest.getInstance("MD5");
                            md.update(parts[1].getBytes());
                            byte[] digest = md.digest();
                            byte[] bytesEncoded = Base64.encodeBase64(digest);
                            localPath = parts[0]+"_"+new String(bytesEncoded);
                        }
                    }
                    downloadResource(u,localPath);
                    anchor.attr("href",localPath);
                }
            }
        }
        catch ( Exception e )
        {
            throw new PsefException(e);
        }
    }
    /**
     * Filter the entire document
     * @return the filtered document
     * @throws PsefException 
     */
    public String filter() throws PsefException
    {
        Document doc = Jsoup.parse(src);
        System.out.println("Filtering scripts");
        filterScripts( doc );
        System.out.println("Filtering styles");
        filterStyles( doc );
        System.out.println("Filtering links");
        filterLinks( doc );
        System.out.println("Filtering anchors");
        filterAnchors( doc );
        // write converted dom back to a string
        StringWriter sw = new StringWriter(src.length());
        PrintWriter writer = new PrintWriter(sw);
        writer.write( doc.html() ) ;
        writer.flush();
        writer.close(); 
        return sw.toString();
    }
}
