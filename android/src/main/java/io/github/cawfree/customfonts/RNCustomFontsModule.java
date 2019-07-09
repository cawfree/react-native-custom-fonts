package io.github.cawfree.customfonts;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableNativeArray;
import com.facebook.react.bridge.ReadableNativeMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.ChoreographerCompat;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.Runnable;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.os.AsyncTask;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.graphics.Typeface;
import android.util.Log;

/** A React Native module used to cache fonts specified via a network connection. */
public class RNCustomFontsModule extends ReactContextBaseJavaModule {

  /* Static Declarations. */
  private static final String TAG                 = "RNCustomFonts";

  /** A generic callback class. */
  private static interface ICallback <T> {
    /** Called on success of the asynchronous action. */
    public void onSuccess(final T pT);
    /** Called when the asynchronous action has failed. */
    public void onFailure(final Exception pException);
  }

  // TODO: be sure these don't get kept in memory
  /** A class which accepts a Promise, and decides to resolve or reject once all of it's dependencies have been resolved. */
  private static class PendingFontFace {
    /* Member Variables. */
    private final Promise           mPromise;
    private final Map<File, String> mPending;
    /** Constructor. */
    protected PendingFontFace(final Promise pPromise, final Map<File, String> pPending) {
      // Initialize Member Variables.
      this.mPromise = pPromise;
      this.mPending = pPending;
    }
    /** A call to define that a result for a FontFace we're interested in has resolved. */
    protected final void onResolutionOf(final File pFile, final String pString, final boolean pIsSuccessful) { 
      // Synchronize upon ourself.
      synchronized(this) {
        // Was the transaction not successful?
        if (!pIsSuccessful) {
          // TODO: add some logging output
        }
        // Ensure we're interested in this type of File.
        if (this.getPending().containsKey(pFile)) {
          // Remove the File from Pending.
          this.getPending().remove(pFile);
        }
        // Have we finished waiting for all pending transactions?
        if (this.getPending().size() == 0) {
          // Assert that we've finished pending.
          this.onFinishedPending(pIsSuccessful);
        }
      }
    }
    // TODO: create a list of errors and successes to be handled back in js for resolve/reject functionality
    /** Called by the class once waiting for the dependencies has finished. */
    protected void onFinishedPending(final boolean pIsSuccessful) {
      this.getPromise().resolve(
        Arguments.createMap()
      );
    }
    /* Getters. */
    private final Promise getPromise() {
      return this.mPromise;
    }
    private final Map<File, String> getPending() {
      return this.mPending;
    }
  }

  /** Writes the contents of an InputStream to an OutputStream. */
  private static final void channelStreams(final InputStream pInputStream, final OutputStream pOutputStream) throws IOException {
    // Declare the read buffer. (TODO: Configurable length?)
    final byte[] bytes        = new byte[1024];
    // Allocate a variable to track how many bytes we read.
          int    numberOfBytes = 0;
    // Iteratively read the streams.
    while((numberOfBytes = pInputStream.read(bytes)) != -1) {
      // Write the data to the OutputStream.
      pOutputStream.write(bytes, 0, numberOfBytes);
    }
    // Ensure all of the bytes are written.
    pOutputStream.flush();
  }

  /** Downloads a file from the network to the specified file path. */
  private static final void downloadFileTo(
      final String pUri,
      final File pFile,
      final ICallback<Void> pCallback
  ) throws IOException {
    // Allocate an AsyncTask to handle file downloads.
    final AsyncTask<Void, Void, Exception> lAsyncTask = new AsyncTask<Void, Void, Exception>() {
      /** Implements the file download. */
      @Override protected final Exception doInBackground(final Void ... pIsUnused) {
        // Fetch the parent directory.
        final File          lParent        = pFile.getParentFile();
        // Declare the I/O dependencies.
        InputStream  lInputStream  = null;
        OutputStream lOutputStream = null;
        try {
          // Fetch the Url.
          final URL           lUrl           = new URL(pUri);
          final URLConnection lUrlConnection = lUrl.openConnection();
          // Establish the connection.
          lUrlConnection.connect();
          // Does the target directory not yet exist? (This would be on first file download.)
          if (!lParent.exists()) {
            // Ensure the directory can be written.
            lParent.mkdirs();
          }
          // Attempt to allocate the I/O Streams.
          lInputStream  = new BufferedInputStream(lUrl.openStream());
          lOutputStream = new FileOutputStream(pFile);
          // Write the contents of the InputStream to the OutputStream.
          RNCustomFontsModule.channelStreams(lInputStream, lOutputStream);
          // Return without error.
          return null;
        }
        catch(final Exception pException) {
          // Prevent memory leaks, if possible.
          if ( lInputStream != null) { try {  lInputStream.close(); } catch(final Exception pIgnoredException) { /* Ignore. */ } }
          if (lOutputStream != null) { try { lOutputStream.close(); } catch(final Exception pIgnoredException) { /* Ignore. */ } }
          // Return the Exception to onPostExecute.
          return pException;
        }
      }
      /** Communicates the result back to the caller along the UI thread. */
      @Override public final void onPostExecute(final Exception pException) {
        // Did the application encounter an Exception?
        if (pException != null) {
          // Inform the Callback.
          pCallback.onFailure(pException);
        } else {
          // Assert that the task was successful.
          pCallback.onSuccess(null);
        }
      }
      /* Unused overrides. */
      @Override protected final void onPreExecute() {}
    };
    // Execute the AsyncTask along an executor.
    lAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  /** Determines the file extension of the specified String. */
  private static final String getFileExtensionOf(final String pString) {
    // Fetch the final index.
    final int i = pString.lastIndexOf('.');
    // Return the remainder of the string (inclusive).
    return pString.substring(i);
  }

  /** Returns the directory where cached fonts are stored. */
  private static final File getCustomFontsDirectory(final Context pContext) throws IOException {
    return new File(
      pContext.getFilesDir() + File.separator + RNCustomFontsModule.TAG
    );
  }

  // TODO: Implement further inspection methods, i.e. check valid string etc, should return a Reason.
  /** Defines whether a FontFace is erroneous. */
  private static final boolean isFontFaceInError(final ReadableMap pFontFace) {
    // Fetch the data dependencies.
    final String lUri        = pFontFace.getString("uri");
    final String lFontFamily = pFontFace.getString("fontFamily");
    final String lFontWeight = pFontFace.getString("fontWeight");
    // Determine whether the font is configured correctly.
    return lUri == null || lFontFamily == null;
  }

  /** Returns a list of only the valid FontFaces from a specified ReadableArray of FontFaces,*/
  private static final List<ReadableMap> getValidFontFaces(final ReadableArray pFontFaces) {
    // Declare the List to return,
    final List<ReadableMap> lFontFaces = new ArrayList();
    // Iterate the ReadableArray.
    for (int i = 0; i < pFontFaces.size(); i += 1) {
      // Fetch the FontFace.
      final ReadableMap lFontFace = pFontFaces.getMap(i);
      // If it is valid, add to the FontFaces to return.
      if (!RNCustomFontsModule.isFontFaceInError(lFontFace)) {
        // Buffer the FontFace.
        lFontFaces.add(lFontFace);
      }
    }
    // Return the FontFaces.
    return lFontFaces;
  }

  /** Returns a deterministic file location of a FontFace.*/
  private static final File getCustomFontFile(final Context pContext, final String pUri, final String pFontFamily, final String pFontWeight) throws IOException {
    return new File(
      RNCustomFontsModule.getCustomFontsDirectory(pContext) + File.separator + pFontFamily + "-" + pFontWeight + RNCustomFontsModule.getFileExtensionOf(pUri)
    );
  }

  /** Converts a FontFace into an equivalent Map. */
  private static final Map<File, String> getPendingMap(final ReactApplicationContext pReactApplicationContext, final List<ReadableMap> pFontFaces) throws IOException {
    // Allocate the PendingMap.
    final Map<File, String> lPendingMap = new HashMap();
    // Iterate the FontFaces.
    for (int i = 0; i < pFontFaces.size(); i += 1) {
      // Fetch the FontFace.
      final ReadableMap lFontFace = pFontFaces.get(i);
      // Fetch the Uri.
      final String      lUri      = lFontFace.getString("uri");
      // Buffer the corresponding implementation into the PendingMap.
      lPendingMap.put(
        RNCustomFontsModule.getCustomFontFile(pReactApplicationContext, lUri, lFontFace.getString("fontFamily"), lFontFace.getString("fontWeight")),
        lUri
      );
    }
    // Return the PendingMap.
    return lPendingMap;
  }

  /** Declares the structure of the FontFamilies sourced by the fontFaces prop. */
  private static final Map<String, Map<String, String>> getFontFamilies(final List<ReadableMap> pFontFaces) {
    // Declare the Map. (<fontFamily, <fontWeight, uri>)
    final Map<String, Map<String, String>> lFontFamilies = new HashMap();
    // Iterate the FontFaces.
    for (int i = 0; i < pFontFaces.size(); i += 1) {
      // Fetch the FontFace.
      final ReadableMap lFontFace   = pFontFaces.get(i);
      // Fetch the FontFamily.
      final String      lFontFamily = lFontFace.getString("fontFamily");
      final String      lUri        = lFontFace.getString("uri");
            String      lFontWeight = lFontFace.getString("fontWeight");
      // Attempt to find the FontFamilyMap.
      Map<String, String> lFontFamilyMap = lFontFamilies.get(lFontFamily);
      // Does it not yet exist?
      if (lFontFamilyMap == null) {
        // Allocate the Map.
        lFontFamilyMap = new HashMap();
        // Buffer the FontFamilyMap into the FontFamilies so that it can be referenced in future iterations.
        lFontFamilies.put(lFontFamily, lFontFamilyMap);
      }
      // Instantiate the uri for this fontWeight in the current FontFamilyMap.
      lFontFamilyMap.put(lFontWeight, lUri);
    }
    // Return the accumulated FontFamilies.
    return lFontFamilies;
  }

  /* Member Variables. */
  private final Map<File, Typeface>              mTypefaces;
  private final Map<File, String>                mUris;
  private final Map<File, List<PendingFontFace>> mPendingFontFaces;
  private final Map<String, Map<String, String>> mFontFamilies;

  /** Default constructor. */
  public RNCustomFontsModule(final ReactApplicationContext pReactApplicationContext) {
    // Implement the parent.
    super(pReactApplicationContext);
    // Initialize member variables.
    this.mTypefaces        = new HashMap<File, Typeface>();
    this.mUris             = new HashMap<File, String>();
    this.mPendingFontFaces = new HashMap<File, List<PendingFontFace>>();
    this.mFontFamilies     = new HashMap<String, Map<String, String>>();
  }

  @ReactMethod
  public final void onFontFacesChanged(
    final ReadableArray pReadableArray,
    final Promise pPromise
  ) {
    // Filter the FontFaces.
    final List<ReadableMap> lFontFaces = RNCustomFontsModule.getValidFontFaces(pReadableArray); 
    // Ensure we update the FontFamilies.
    this.isolateExecutionOf(new Runnable() { @Override public final void run() {
      // Fetch the FontFamilies.
      final Map<String, Map<String, String>> lFontFamilies = RNCustomFontsModule.this.getFontFamilies();
      // Clear the existing FontFamilies that are latched by the member variable.
      RNCustomFontsModule.this.getFontFamilies().clear();
      // Buffer the new FontFamilies reference into the instance.
      RNCustomFontsModule.this.getFontFamilies().putAll(RNCustomFontsModule.getFontFamilies(lFontFaces));
    }});
    // Are there any FontFaces to manage?
    if (lFontFaces.size() == 0) {
      // Then terminate early; the below arcitecture works by interacting with a valid PendingFontFace; but there's nothing to pend on for this case.
      pPromise.resolve(
        // TODO: need to test this case
        Arguments.createMap()
      );
    } else {
      // Declare our logic dependencies.
      PendingFontFace   lPendingFontFace = null;
      Map<File, String> lPendingMap      = null;
      // Attempt to allocate these dependencies.
      try {
        // Declare the PendingMap.
        lPendingMap = RNCustomFontsModule.getPendingMap(
          this.getReactApplicationContext(),
          lFontFaces
        );
        // Allocate the PendingFontFace. (Notice that we make a safe clone of the PendingMap, as the PendingFontFace will attempt to modify it!)
        lPendingFontFace = new PendingFontFace(pPromise, new HashMap<File, String>(lPendingMap));
      }
      catch (final Exception pException) {
        // Propagate the error back to the caller.
        pPromise.reject(pException);
      }
      finally {
        // Did we manage to allocate the logic dependencies?
        if (lPendingFontFace != null && lPendingMap != null) {
          // Iterate the PendingFontFaces.
          for (final Map.Entry<File, String> lEntry : lPendingMap.entrySet()) {
            // Fetch the File and Uri.
            final File   lFile = lEntry.getKey();
            final String lUri  = lEntry.getValue();
            // Determine whether we've encountered this font before.
            final boolean isPreviouslyEncountered = RNCustomFontsModule.this.didPreviouslyEncounter(
              lFile,
              lUri
            );
            // Did we previously encounter it?
            if (isPreviouslyEncountered) {
              // So, it looks like we've seen this FontFace before. Do we have a result for the Font?
              final boolean  lHasTypefaceResult = RNCustomFontsModule.this.getTypefaces().containsKey(lFile);
              // Is there a result for the Typeface?
              if (lHasTypefaceResult) {
                // Attempt to fetch the Typeface. Note, this could be either a valid typeface, or null, to indicate a failed transaction.
                final Typeface lTypeface = RNCustomFontsModule.this.getTypefaces().get(lFile);
                // Is the result non-null? This means we actually have the Typeface!
                if (lTypeface != null) {
                  // Assert that we already have the FontFace.
                  lPendingFontFace.onResolutionOf(lFile, lUri, true);
                } else {
                  // Indicate that we attempted this transaction before, and it had failed.
                  lPendingFontFace.onResolutionOf(lFile, lUri, false);
                }
              } else {
                // Allow the PendingFontFace to await resolution.
                RNCustomFontsModule.this.schedulePendingFontFace(lFile, lUri, lPendingFontFace);
              }
            } else {
              // Has the referenced file been encountered before?
              final boolean lIsAttemptingOverwrite = RNCustomFontsModule.this.getUris().containsKey(lFile);
              // Is the user attempting an overwrite? (i.e. the File reference has been enountered, but the uri has changed.
              if (lIsAttemptingOverwrite) {
                // TODO: It should be possible to do this.
                lPendingFontFace.onResolutionOf(lFile, lUri, false);
              } else {
                // Let's mark the File as encountered.
                RNCustomFontsModule.this.assertEncountered(lFile, lUri);
                // Allow the PendingFontFace to await resolution.
                RNCustomFontsModule.this.schedulePendingFontFace(lFile, lUri, lPendingFontFace);
                // Attempt to download the file.
                try {
                  RNCustomFontsModule.this.manageDownloadOf(lFile, lUri);
                } catch (final Exception pException) {
                  // Catch the exception and clear any pending listeners on this file.
                  RNCustomFontsModule.this.onUpdatePendingFontFaces(lFile, lUri, false);
                }
              }
            }
          }
        }
      }
    }
  }

  /** Iterates through all of the PendingFontFaces for a given file and updates them with the result. */
  private final void onUpdatePendingFontFaces(final File pFile, final String pUri, final boolean pIsSuccessful) {
    // Fetch the PendingFontFaces interested in this result.
    final List<PendingFontFace> lPendingFontFaces = this.getPendingFontFaces().get(pFile);
    // Are there any listeners at all?
    if (!lPendingFontFaces.isEmpty()) {
      // Iterate the PendingFontFaces.
      for (final PendingFontFace lPendingFontFace : lPendingFontFaces) {
        // Update the PendingFontFace.
        lPendingFontFace.onResolutionOf(pFile, pUri, pIsSuccessful);
      }
      // Finally, clear the PendingFontFaces from listening to further transactions. (Allows the List to be reused.)
      lPendingFontFaces.clear();
    }
  }

  /** Implements a download operation and updates any observers that are interested in the result. */
  private final void manageDownloadOf(final File pFile, final String pUri) throws IOException {
    RNCustomFontsModule.downloadFileTo(
      pUri,
      pFile,
      new ICallback<Void>() {
        /** On success... */
        @Override public final void onSuccess(final Void pIsUnused) {
          // Synchronize upon ourself.
          RNCustomFontsModule.this.isolateExecutionOf(new Runnable() { @Override public final void run() {
            // Attempt to create the Typeface.
            Typeface lTypeface = null;
            try {
              // Attempt to allocate a Typeface.
              lTypeface = Typeface.createFromFile(pFile);
              // Is the Typeface valid?
              if (lTypeface != null) {
                // Buffer the Typeface.
                RNCustomFontsModule.this.getTypefaces().put(pFile, lTypeface);
                // Inform listeners of the success.
                RNCustomFontsModule.this.onUpdatePendingFontFaces(pFile, pUri, true);
              }
            } catch (final Exception pException) {
              // Register a null value of the File, to indicate that the Typeface for this file is invalid.
              RNCustomFontsModule.this.getTypefaces().put(pFile, null);
              // We've failed to create the Typeface for the given file.
              RNCustomFontsModule.this.onUpdatePendingFontFaces(pFile, pUri, false);
            }
          } });
        }
        /** On failure... */
        @Override public final void onFailure(final Exception pException) {
          // Synchronize upon ourself.
          RNCustomFontsModule.this.isolateExecutionOf(new Runnable() { @Override public final void run() {
            // Inform listeners of the failure.
            RNCustomFontsModule.this.onUpdatePendingFontFaces(pFile, pUri, false);
          } });
        }
      }
    );
  }

  /** Defines whether we've 'seen' a particular FontFace before. (Prevents duplicate requests.) */
  private final boolean didPreviouslyEncounter(final File pFile, final String pUri) {
    // Have we seen this file key before?
    if (this.getUris().containsKey(pFile)) {
      // Fetch the uri that rests there.
      final String lUri = this.getUris().get(pFile);
      // We can only mark a font as "previously encountered" if the uri it references matches the requester for the same file reference.
      return pUri.equals(lUri);
    }
    return false;
  }

  /** Asserts that we've seen a particular font. */
  private final void assertEncountered(final File pFile, final String pUri) {
    // Retain the identifying information of the FontFace.
    this.getUris().put(pFile, pUri);
  }

  /** Schedules a PendingFontFace to wait for events along a particular resource. */
  private final void schedulePendingFontFace(final File pFile, final String pUri, final PendingFontFace pPendingFontFace) {
    // Attempt to fetch the corresponding List of listeners.
    List<PendingFontFace> lPendingFontFaces = this.getPendingFontFaces().get(pFile);
    // Does the List not yet exist?
    if (lPendingFontFaces == null) {
      // Allocate the PendingFontFaces.
      lPendingFontFaces = new ArrayList();
      // Ensure these are buffered within the map for future fetches.
      this.getPendingFontFaces().put(pFile, lPendingFontFaces);
    }
    // Add the PendingFontFace to the List of PendingFontFaces.
    lPendingFontFaces.add(pPendingFontFace);
  }

  /** Returns the uri for a specified FontFamily and FontWeight, or null if it cannot be found. */
  private final String getUriFor(final String pFontFamily, final String pFontWeight) {
    // Attempt to fetch the FontFamily declaration.
    final Map<String, String> lFontFamily = this.getFontFamilies().get(pFontFamily);
    // Does the Font Family exist?
    if (lFontFamily != null) {
      // Attempt to find the uri for the specified FontWeight. This will be null if the FontWeight does not belong to the family.
      return lFontFamily.get(pFontWeight);
    }
    // The uri could not be found.
    return null;
  }

  /** Assigns the Typeface to a TextView. */
  // https://github.com/facebook/react-native/issues/17968
  private final void assignTypefaceTo(final Typeface pTypeface, final TextView pTextView) {
    // TODO: verify activity works first
    this.getReactApplicationContext().getCurrentActivity().runOnUiThread(new Runnable() { @Override public final void run() {
      // Assign the Typeface.
      pTextView.setTypeface(pTypeface);
      // Ensure we update the View layout.
      pTextView.requestLayout();
    } });
  }
  
  @ReactMethod
  public final void onRequestFontFamily(
    final int pViewHandle,
    final String pFontFamily,
    final String pFontWeight,
    final Promise pPromise
  ) {
    // Is there an active Activity we can use to interrogate the layout?
    if (this.getReactApplicationContext().hasCurrentActivity()) {
      // Attempt to find the equivalent View referenced by the ViewHandle.
      final View lView = this.getReactApplicationContext()
        .getCurrentActivity()
        .findViewById(pViewHandle);
      if (lView != null) {
        try {
          // Attempt to coerce the TextView.
          final TextView lTextView = (TextView)(lView);
          // Synchronize along ourself.
          this.isolateExecutionOf(new Runnable() { @Override public final void run() {
            try {
              // Attempt to find the Uri for the specified FontFamily and FontWeight.
              final String lUri  = RNCustomFontsModule.this.getUriFor(pFontFamily, pFontWeight);
              // Could the Uri not be found?
              if (lUri == null) {
                // Throw an error; the user has specified a non-existent Font.
                throw new Exception(
                  "Attempted to use fontFamily \"" + pFontFamily + "\" with fontWeight \"" + pFontWeight + "\", but this was is not defined. This configuration should be present in the fontFaces prop."
                );
              }
              // Find the corresponding File for the specified Uri.
              final File lFile = RNCustomFontsModule.getCustomFontFile(
                RNCustomFontsModule.this.getReactApplicationContext(),
                lUri,
                pFontFamily,
                pFontWeight
              );
              // Determine whether we've encountered this font before.
              final boolean isPreviouslyEncountered = RNCustomFontsModule.this.didPreviouslyEncounter(
                lFile,
                lUri
              );
              // Have we previously encountered this font?
              if (isPreviouslyEncountered) {
                // Has the Typeface finished processing?
                if (RNCustomFontsModule.this.getTypefaces().containsKey(lFile)) {
                  // Fetch the Typeface.
                  final Typeface lTypeface = RNCustomFontsModule.this.getTypefaces().get(lFile);
                  // Is the Typeface valid?
                  if (lTypeface != null) {
                    // Attempt to assign the Typeface to the TextView.
                    RNCustomFontsModule.this.assignTypefaceTo(lTypeface, lTextView);
                    // Resolve without error.
                    pPromise.resolve(
                      Arguments.createMap()
                    );
                  } else {
                    // The Typeface is not valid; further network requests must fail.
                    pPromise.reject(
                      new Exception(
                        "Unable to use fontFamily \"" + pFontFamily + "\" at fontWeight \"" + pFontWeight + "\", because the application failed to either download or process the content served by uri \"" + lUri + "\"."
                      )
                    );
                  }
                } else {
                  // The font is still processing. We need to wait until it becomes ready.
                  RNCustomFontsModule.this.schedulePendingFontFace(
                    lFile,
                    lUri,
                    new PendingFontFace(
                      pPromise,
                      new HashMap<File, String>() { { this.put(lFile, lUri); }}
                    ) {
                      @Override protected final void onFinishedPending(final boolean pIsSuccessful) {
                        // Was the fetch successful?
                        if (pIsSuccessful) {
                          // Fetch the Typeface.
                          final Typeface lTypeface = RNCustomFontsModule.this.getTypefaces().get(lFile);
                          // Assign the Typeface to the TextView.
                          RNCustomFontsModule.this.assignTypefaceTo(lTypeface, lTextView);
                        }
                        // Implement the parent.
                        super.onFinishedPending(pIsSuccessful);
                      }
                    }
                  );
                }
              } else {
                // TODO: we should probably never get here...
                // The application has no knowledge of this font!
                throw new Exception(
                  "Attempted to use a font which the provider has no knowledge of."
                );
              }
            } catch (final Exception pException) {
              // Delegate the Exception to the caller.
              pPromise.reject(
                pException
              );
            }
          } });
        }
        catch (final Exception pException) {
          // Delegate the Exception to the caller.
          pPromise.reject(
            pException
          );
        }
      } else {
        pPromise.reject(
          new Exception(
            "Failed to findViewById using handle 0x" + Integer.toHexString(pViewHandle) + "."
          )
        );
      }
    } else {
      pPromise.reject(
        new Exception(
          "Unable to requestFontFamily; the ReactApplicationContext is currently available."
        )
      );
    }
  }

  /** Executes the runnable within a synchronized context of this class.  */
  private final void isolateExecutionOf(final Runnable pRunnable) {
    // Synchronize on this instance,
    synchronized(this) {
      // Execute the Runnable.
      pRunnable.run();
    }
  }

  /* Getters. */
  @Override public String getName() {
    return RNCustomFontsModule.TAG;
  }

  private final Map<File, Typeface> getTypefaces() {
    return this.mTypefaces;
  }

  private final Map<File, String> getUris() {
    return this.mUris;
  }

  private final Map<File, List<PendingFontFace>> getPendingFontFaces() {
    return this.mPendingFontFaces;
  }
  
  private final Map<String, Map<String, String>> getFontFamilies() {
    return this.mFontFamilies;
  }
  
}
