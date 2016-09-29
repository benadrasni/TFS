package au.com.tfsltd.invertebrateKey;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Detail activity
 *
 * Created by adrian on 17.9.2016.
 */
public class DetailActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static final int REQUEST_TAKE_PHOTO = 1;

    private GoogleApiClient mGoogleApiClient;

    FloatingActionButton fab;
    FloatingActionButton fabPhoto;
    FloatingActionButton fabShare;

    //Animations
    Animation showFabPhoto;
    Animation hideFabPhoto;
    Animation showFabShare;
    Animation hideFabShare;

    private boolean FAB_Status = false;
    private Location mLastLocation;
    private Uri mCurrentPhotoUri;
    private String mCurrentPhotoPath;
    private String possibleAnswers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String path = getIntent().getExtras().getString(Constants.PATH);
        assert path != null;

        setContentView(R.layout.activity_detail);

        final TextView detailView = (TextView) findViewById(R.id.detail);
        final LinearLayout photosLayout = (LinearLayout) findViewById(R.id.photos);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabPhoto = (FloatingActionButton) findViewById(R.id.fab_photo);
        fabShare = (FloatingActionButton) findViewById(R.id.fab_share);

        showFabPhoto = AnimationUtils.loadAnimation(getApplication(), R.anim.fab_photo_show);
        hideFabPhoto = AnimationUtils.loadAnimation(getApplication(), R.anim.fab_photo_hide);
        showFabShare = AnimationUtils.loadAnimation(getApplication(), R.anim.fab_share_show);
        hideFabShare = AnimationUtils.loadAnimation(getApplication(), R.anim.fab_share_hide);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FAB_Status) {
                    hideFAB();
                } else {
                    expandFAB();
                }
                FAB_Status = !FAB_Status;
            }
        });

        fabPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplication(), "Floating Action Button 3", Toast.LENGTH_SHORT).show();
            }
        });

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mFirebaseRef = database.getReference(path);

        mFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map detailMap = (Map) dataSnapshot.getValue();

                detailView.setText((String) detailMap.get(Constants.FIELD_ENDPOINT_INFO));
                possibleAnswers = (String) detailMap.get(Constants.FIELD_POSSIBLE_ANSWERS);

                List<String> photos = (List<String>) detailMap.get(Constants.FIELD_PHOTOS);
                for (String photoUrl : photos) {
                    photosLayout.addView(createPhotoImage(photoUrl));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getMessage());
            }
        });

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            final Observation observation = new Observation();
            observation.setDate(new Date());
            if (mLastLocation != null) {
                observation.setLatitude(mLastLocation.getLatitude());
                observation.setLongitude(mLastLocation.getLongitude());
            }

            TFSApp tfsApp = (TFSApp) getApplication();
            if (tfsApp.isNetworkAvailable(getApplicationContext())) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl(Constants.STORAGE);

                StorageReference imagesRef = storageRef.child(Constants.PHOTOS_STORAGE + Constants.PATH_SEPARATOR
                        + mCurrentPhotoUri.getLastPathSegment());
                UploadTask uploadTask = imagesRef.putFile(mCurrentPhotoUri);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        observation.setPhotoPath(mCurrentPhotoPath);
                        saveObservation(observation);
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        observation.setPhotoPath(taskSnapshot.getDownloadUrl().getPath());
                        saveObservation(observation);
                    }
                });

            } else {
                observation.setPhotoPath(mCurrentPhotoPath);
                saveObservation(observation);
            }
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private ImageView createPhotoImage(final String photoPath) {
        ImageView photo = new ImageView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(20, 10, 20, 10);
        photo.setLayoutParams(lp);
        photo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        photo.setAdjustViewBounds(true);

        String imageKey = Constants.PATH_SEPARATOR + photoPath + ".jpg";
        Bitmap myBitmap = ((TFSApp) getApplication()).getBitmapFromMemCache(imageKey);
        if (myBitmap == null) {
            File imgFile = new File(this.getApplicationContext().getFilesDir() + imageKey);
            if (imgFile.exists()) {
                myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ((TFSApp) getApplication()).addBitmapToMemoryCache(imageKey, myBitmap);
                photo.setImageBitmap(myBitmap);
            }
        } else {
            photo.setImageBitmap(myBitmap);
        }

        return photo;
    }

    private void expandFAB() {

        CoordinatorLayout.LayoutParams layoutParamsPhoto = (CoordinatorLayout.LayoutParams) fabPhoto.getLayoutParams();
        layoutParamsPhoto.bottomMargin += (int) (fabPhoto.getHeight() * 1.5);
        fabPhoto.setVisibility(View.VISIBLE);
        fabPhoto.setLayoutParams(layoutParamsPhoto);
        fabPhoto.startAnimation(showFabPhoto);
        fabPhoto.setClickable(true);

        CoordinatorLayout.LayoutParams layoutParamsShare = (CoordinatorLayout.LayoutParams) fabShare.getLayoutParams();
        layoutParamsShare.bottomMargin += (int) (fabShare.getHeight() * 3.0);
        fabShare.setVisibility(View.VISIBLE);
        fabShare.setLayoutParams(layoutParamsShare);
        fabShare.startAnimation(showFabShare);
        fabShare.setClickable(true);
    }


    private void hideFAB() {

        CoordinatorLayout.LayoutParams layoutParamsPhoto = (CoordinatorLayout.LayoutParams) fabPhoto.getLayoutParams();
        layoutParamsPhoto.bottomMargin -= (int) (fabPhoto.getHeight() * 1.5);
        fabPhoto.setLayoutParams(layoutParamsPhoto);
        fabPhoto.startAnimation(hideFabPhoto);
        fabPhoto.setClickable(false);
        fabPhoto.setVisibility(View.INVISIBLE);

        CoordinatorLayout.LayoutParams layoutParamsShare = (CoordinatorLayout.LayoutParams) fabShare.getLayoutParams();
        layoutParamsShare.bottomMargin -= (int) (fabShare.getHeight() * 3.0);
        fabShare.setLayoutParams(layoutParamsShare);
        fabShare.startAnimation(hideFabShare);
        fabShare.setClickable(false);
        fabShare.setVisibility(View.INVISIBLE);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();

        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "au.com.tfsltd.invertebrateKey.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                mCurrentPhotoUri = photoURI;
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void saveObservation(Observation observation) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mFirebaseRef = database.getReference();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        assert auth.getCurrentUser() != null;
        String userUid = auth.getCurrentUser().getUid();

        long time = new Date().getTime();

        mFirebaseRef.child(Constants.FIELD_OBSERVATIONS).child(userUid).child(possibleAnswers).child(""+time).setValue(observation);
    }

}
