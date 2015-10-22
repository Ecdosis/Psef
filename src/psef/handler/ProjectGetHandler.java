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

import calliope.core.Utils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;
import psef.exception.PsefException;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import psef.constants.Params;
import org.json.simple.*;
import java.net.URL;
import java.io.InputStream;
import java.net.URLConnection;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.utils.IOUtils;

/**
 * Download an entire project as a .tar.gz archive
 * @author desmond
 */
public class ProjectGetHandler extends PsefGetHandler
{
    String docid;
    File root;
    static final int BUF_SIZE = 32768;
    private void writeUrlContents( String url, String path ) 
        throws PsefException
    {
        try
        {
            URL u = new URL(url);
            URLConnection connection = u.openConnection();
            InputStream is = connection.getInputStream();
            byte[] data = Utils.readStream(is);
            File dst;
            if ( path.length()==0 )
                dst = new File( root, "index.html" );
            else
                dst = new File(path);
            if ( dst.exists() )
                dst.delete();
            dst.createNewFile();
            FileOutputStream out = new FileOutputStream(dst);
            out.write(data);
            out.close();
        }
        catch ( Exception e )
        {
            throw new PsefException(e);
        }
    }
    /**
     * Get the last non-zero component in the site url
     * @param siteUrl the site url or other url
     * @return the last path component
     */
    private String getLastPart( URL siteU ) throws Exception
    {
        String[] parts = siteU.getPath().split("/");
        for ( int i=parts.length-1;i>=0;i-- )
            if ( parts[i].length()>0 )
                return parts[i];
        return "";
    }
    /**
     * Subtract the paths of two URLs starting from the start
     * @param one the one to subtract
     * @param two the one to subtract from
     * @return the difference
     */
    private String urlDiff( URL one, URL two )
    {
        String onePath = one.getPath();
        String twoPath = two.getPath();
        if ( twoPath.startsWith(onePath) )
            return twoPath.substring(onePath.length());
        else
            return twoPath;
    }
    /**
     * Add files to a tar.gz archive recursively
     * @param tOut the tar archive output stream
     * @param path the absolute path to the file/dir to add
     * @param base the base path inside the tar.gz archive
     * @throws IOException 
     */
    private void addFilesToTarGz(
         TarArchiveOutputStream tOut, String path, String base) throws IOException 
    {
        File f = new File(path);
        String entryName = base + f.getName();
        TarArchiveEntry tarEntry = new TarArchiveEntry(f, entryName);
        tOut.putArchiveEntry(tarEntry);

        if (f.isFile()) 
        {
            IOUtils.copy(new FileInputStream(f), tOut);
            tOut.closeArchiveEntry();
        } 
        else 
        {
            tOut.closeArchiveEntry();
            File[] children = f.listFiles();
            if (children != null){
                for (File child : children) {
                    addFilesToTarGz(tOut, child.getAbsolutePath(), 
                        entryName + "/");
                }
            }
        }
    }
    /**
     * Convert the root into a tarball
     * @return file pointing to the temporary tar.gz file
     */
    private File tarRoot() throws IOException
    {
        TarArchiveOutputStream tOut=null;
        GzipCompressorOutputStream gzOut=null;
        BufferedOutputStream bOut=null;
        FileOutputStream fOut=null;
        try 
        {
            File parent = root.getParentFile();
            File dst = new File( parent, root.getName()+".tar.gz" );
            if ( dst.exists() )
            {
                dst.delete();
                dst.createNewFile();
            }
            String tarGzPath = dst.getAbsolutePath();
            fOut = new FileOutputStream(new File(tarGzPath));
            bOut = new BufferedOutputStream(fOut);
            gzOut = new GzipCompressorOutputStream(bOut);
            tOut = new TarArchiveOutputStream(gzOut);
            addFilesToTarGz(tOut, root.getAbsolutePath(), "");
            return dst;  
        } 
        finally
        {
            if ( tOut != null )
            {
                tOut.finish();
                tOut.close();
            }
            if ( gzOut != null )
                gzOut.close();
            if ( bOut != null )
                bOut.close();
            if ( fOut != null )
                fOut.close();
        }
    }
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws PsefException {
        //1. get the project metadata
        try
        {
            Connection conn = Connector.getConnection();
            docid = request.getParameter(Params.DOCID);
            if ( docid == null || docid.length()==0 )
                throw new PsefException("Missing docid");
            String project = conn.getFromDb(Database.PROJECTS,docid);
            JSONObject jObj = (JSONObject)JSONValue.parse( project );
            String site_url = (String)jObj.get(JSONKeys.SITE_URL);
            // 2. create the temporary directory
            if ( site_url != null )
            {
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                URL siteU = new URL(site_url);
                root = new File( tempDir, getLastPart(siteU) );
                if ( !root.exists() )
                    root.mkdir();
                URL current = new URL(site_url);
                String path = urlDiff( siteU, current );
                // 2. get the home page and parse it
                writeUrlContents(site_url, path);
                File archive = tarRoot();
                response.setContentType("application/gzip");
                ServletOutputStream sos = response.getOutputStream();
                byte[] buffer = new byte[BUF_SIZE];
                FileInputStream fis = new FileInputStream(archive);
                BufferedInputStream bis = new BufferedInputStream(fis,BUF_SIZE);
                while ( bis.available() > 0 )
                {
                    int amt = bis.available();
                    bis.read( buffer, 0, amt );
                    sos.write( buffer, 0, amt );
                }
            }
            else
                throw new Exception("project doesn't have a site_url");
        }
        catch ( Exception e )
        {
            throw new PsefException(e);
        }
    }
}
