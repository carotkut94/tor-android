/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

package org.torproject.android.service;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.torproject.android.R;
import org.torproject.android.TorConstants;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class TorBinaryInstaller implements TorServiceConstants {

	
	File installFolder;
	Context context;
	
    private static int isARMv6 = -1;

	public TorBinaryInstaller (Context context, File installFolder)
	{
		this.installFolder = installFolder;
		
		this.context = context;
	}
	
	//		
	/*
	 * Extract the Tor binary from the APK file using ZIP
	 */
	public boolean installFromRaw () throws IOException
	{
		
		InputStream is;
		
		is = context.getResources().openRawResource(R.raw.tor);			
		streamToFile(is,installFolder, TOR_BINARY_ASSET_KEY, false, true);
			
		is = context.getResources().openRawResource(R.raw.torrc);			
		streamToFile(is,installFolder, TORRC_ASSET_KEY, false, false);

		is = context.getResources().openRawResource(R.raw.torrctether);			
		streamToFile(is,installFolder, TORRC_TETHER_KEY, false, false);

		is = context.getResources().openRawResource(R.raw.privoxy);			
		streamToFile(is,installFolder, PRIVOXY_ASSET_KEY, false, false);

		is = context.getResources().openRawResource(R.raw.privoxy_config);			
		streamToFile(is,installFolder, PRIVOXYCONFIG_ASSET_KEY, false, false);
		
		is = context.getResources().openRawResource(R.raw.geoip);			
		streamToFile(is,installFolder, GEOIP_ASSET_KEY, false, true);
	
		return true;
	}
	
	private static void copyAssetFile(Context ctx, String asset, File file) throws IOException, InterruptedException
	{
    	
		DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
		InputStream is = new GZIPInputStream(ctx.getAssets().open(asset));
		
		byte buf[] = new byte[8172];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
	}
	/*
	 * Write the inputstream contents to the file
	 */
    private static boolean streamToFile(InputStream stm, File folder, String targetFilename, boolean append, boolean zip) throws IOException

    {
        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;

        File outFile = new File(folder, targetFilename);

    	OutputStream stmOut = new FileOutputStream(outFile, append);
    	
    	if (zip)
    	{
    		ZipInputStream zis = new ZipInputStream(stm);    		
    		ZipEntry ze = zis.getNextEntry();
    		stm = zis;
    		
    	}
    	
        while ((bytecount = stm.read(buffer)) > 0)

        {

            stmOut.write(buffer, 0, bytecount);

        }

        stmOut.close();
        stm.close();
        
        return true;

    }
	
    //copy the file from inputstream to File output - alternative impl
	public void copyFile (InputStream is, File outputFile)
	{
		
		try {
			outputFile.createNewFile();
			DataOutputStream out = new DataOutputStream(new FileOutputStream(outputFile));
			DataInputStream in = new DataInputStream(is);
			
			int b = -1;
			byte[] data = new byte[1024];
			
			while ((b = in.read(data)) != -1) {
				out.write(data);
			}
			
			if (b == -1); //rejoice
			
			//
			out.flush();
			out.close();
			in.close();
			// chmod?
			
			
			
		} catch (IOException ex) {
			Log.e(TorConstants.TAG, "error copying binary", ex);
		}

	}
	
	

    /**
	 * Check if this is an ARMv6 device
	 * @return true if this is ARMv6
	 */
	private static boolean isARMv6() {
		if (isARMv6 == -1) {
			BufferedReader r = null;
			try {
				isARMv6 = 0;
				r = new BufferedReader(new FileReader("/proc/cpuinfo"));
				for (String line = r.readLine(); line != null; line = r.readLine()) {
					if (line.startsWith("Processor") && line.contains("ARMv6")) {
						isARMv6 = 1;
						break;
					} else if (line.startsWith("CPU architecture") && (line.contains("6TE") || line.contains("5TE"))) {
						isARMv6 = 1;
						break;
					}
				}
			} catch (Exception ex) {
			} finally {
				if (r != null) try {r.close();} catch (Exception ex) {}
			}
		}
		return (isARMv6 == 1);
	}
	
	/**
	 * Copies a raw resource file, given its ID to the given location
	 * @param ctx context
	 * @param resid resource id
	 * @param file destination file
	 * @param mode file permissions (E.g.: "755")
	 * @throws IOException on error
	 * @throws InterruptedException when interrupted
	 */
	private static void copyRawFile(Context ctx, int resid, File file, String mode) throws IOException, InterruptedException
	{
		final String abspath = file.getAbsolutePath();
		// Write the iptables binary
		final FileOutputStream out = new FileOutputStream(file);
		final InputStream is = ctx.getResources().openRawResource(resid);
		byte buf[] = new byte[1024];
		int len;
		while ((len = is.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		out.close();
		is.close();
		// Change the permissions
		Runtime.getRuntime().exec("chmod "+mode+" "+abspath).waitFor();
	}
    /**
	 * Asserts that the binary files are installed in the cache directory.
	 * @param ctx context
     * @param showErrors indicates if errors should be alerted
	 * @return false if the binary files could not be installed
	 */
	public static boolean assertIpTablesBinaries(Context ctx, boolean showErrors) throws Exception {
		boolean changed = false;
		
		// Check iptables_g1
		File file = new File(ctx.getDir("bin",0), "iptables");
		
		if ((!file.exists()) && isARMv6()) {
			copyRawFile(ctx, R.raw.iptables_g1, file, "755");
			changed = true;
		}
		
		// Check iptables_n1
		file = new File(ctx.getDir("bin",0), "iptables");
		if ((!file.exists()) && (!isARMv6())) {
			copyRawFile(ctx, R.raw.iptables_n1, file, "755");
			changed = true;
		}
		
		// Check busybox
		/*
		file = new File(ctx.getDir("bin",0), "busybox_g1");
		if (!file.exists()) {
			copyRawFile(ctx, R.raw.busybox_g1, file, "755");
			changed = true;
		}
		*/
		
		if (changed) {
				Toast.makeText(ctx, R.string.status_install_success, Toast.LENGTH_LONG).show();
			}
		
		return true;
	}
	

}
