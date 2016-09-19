package au.com.tfsltd.tfs;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 *
 * Created by adrian on 18.9.2016.
 */
public class StorageLoadingActivity extends AppCompatActivity {

    private class UnpackZip extends AsyncTask<Void, Integer, Integer> {

        private File _zipFile;
        private int _size;
        private int _processedFiles;

        public UnpackZip(File zipFile) {
            _zipFile = zipFile;
            _processedFiles = 0;
            _size = 0;
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage(getResources().getString(R.string.unzipping_photos));
            progressDialog.setProgress(0);

            try {
                ZipFile zf = new ZipFile(_zipFile.getAbsolutePath());
                _size = zf.size();
                progressDialog.setMax(_size);
            } catch (IOException e) {
                progressDialog.setMax(1);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            int per = (int) (100.0 * ((float)progress[0] / _size));
            progressDialog.setProgress(per);
        }

        @Override
        protected void onPostExecute(Integer result) {
            Intent intent = new Intent(StorageLoadingActivity.this, QuestionActivity.class);
            intent.putExtra(Constants.PATH, Constants.FIELD_QUESTION);
            startActivity(intent);
            progressDialog.dismiss();
            finish();
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            InputStream is;
            ZipInputStream zis;
            int totalSize = 0;
            try {
                String path;
                String filename;
                is = new FileInputStream(_zipFile);
                zis = new ZipInputStream(new BufferedInputStream(is));
                ZipEntry ze;
                byte[] buffer = new byte[1024];
                int count = 0;
                while ((ze = zis.getNextEntry()) != null) {
                    filename = ze.getName();
                    path = StorageLoadingActivity.this.getApplicationContext().getFilesDir() + "/";

                    if (ze.isDirectory()) {
                        File fmd = new File(path + filename);
                        fmd.mkdirs();
                        continue;
                    }

                    FileOutputStream fout = new FileOutputStream(path + filename);

                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                        totalSize += count;
                    }

                    fout.close();
                    zis.closeEntry();
                    _processedFiles++;
                    publishProgress(_processedFiles);
                }

                zis.close();
            } catch (IOException e) {
                e.printStackTrace();
                return 0;
            }

            return totalSize;
        }
    }

    protected ProgressDialog progressDialog;

    protected void loadQuestionActivity() {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        StorageReference storageRef = storage.getReferenceFromUrl(Constants.STORAGE);
        StorageReference photosReference = storageRef.child(Constants.PHOTOS_ZIP);

        final File localFile = new File(getApplicationContext().getFilesDir() + "/" + Constants.PHOTOS_ZIP);

        progressDialog=new ProgressDialog(this);
        progressDialog.setMessage(getResources().getString(R.string.downloading_photos));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(1);
        progressDialog.show();

        photosReference.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                new UnpackZip(localFile).execute();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                int progress = (int) (100.0 * ((float)taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount()));
                progressDialog.setProgress(progress);
            }
        });
    }
}
