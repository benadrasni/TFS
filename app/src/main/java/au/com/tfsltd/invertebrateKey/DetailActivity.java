package au.com.tfsltd.invertebrateKey;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Detail activity
 * <p>
 * Created by adrian on 17.9.2016.
 */
public class DetailActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static final int REQUEST_TAKE_PHOTO = 1;

    private GoogleApiClient mGoogleApiClient;
    private FirebaseDatabase database;

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
    private Date date;
    private String path;
    private String possibleAnswers;
    private String userId;

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
            }
        });

        fabPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFAB();
                dispatchTakePictureIntent();
            }
        });

        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFAB();
                callShareActivity();
            }
        });

        database = FirebaseDatabase.getInstance();
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

        FirebaseAuth auth = FirebaseAuth.getInstance();
        assert auth.getCurrentUser() != null;
        userId = auth.getCurrentUser().getUid();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            final Observation observation = new Observation();
            observation.setPhotoPath(path + mCurrentPhotoUri.getLastPathSegment());
            observation.setDate(date);
            if (mLastLocation != null) {
                observation.setLatitude(mLastLocation.getLatitude());
                observation.setLongitude(mLastLocation.getLongitude());
            }

            TFSApp tfsApp = (TFSApp) getApplication();
            if (tfsApp.isNetworkAvailable(getApplicationContext())) {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference storageRef = storage.getReferenceFromUrl(Constants.STORAGE);

                StorageReference imagesRef = storageRef.child(Constants.PHOTOS_STORAGE + path + mCurrentPhotoUri.getLastPathSegment());
                UploadTask uploadTask = imagesRef.putFile(mCurrentPhotoUri);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        observation.setUploaded(false);
                        saveObservation(observation);
                        Toast.makeText(getApplication(), getResources().getString(R.string.photo_saved_not_uploaded), Toast.LENGTH_SHORT).show();
                        callShareActivity();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        observation.setUploaded(true);
                        saveObservation(observation);
                        Toast.makeText(getApplication(), getResources().getString(R.string.photo_saved_uploaded), Toast.LENGTH_SHORT).show();
                        callShareActivity();
                    }
                });

            } else {
                saveObservation(observation);
                Toast.makeText(getApplication(), getResources().getString(R.string.photo_saved), Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                }
            }
        }
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
//                myBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imgFile.getAbsolutePath()),
//                        Constants.IMAGE_SIZE, Constants.IMAGE_SIZE);
                myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ((TFSApp) getApplication()).addBitmapToMemoryCache(imageKey, myBitmap);
                photo.setImageBitmap(myBitmap);
            } else {
                photo.setImageDrawable(ContextCompat.getDrawable(this.getApplicationContext(), R.drawable.no_image_available));
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
        FAB_Status = true;
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
        FAB_Status = false;
    }

    private File createImageFile() throws IOException {
        date = new Date();
        path = Constants.PATH_SEPARATOR + userId + Constants.PATH_SEPARATOR;

        String imageFileName = "JPEG_" + date.getTime() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES + path);

        return File.createTempFile(imageFileName, ".jpg", storageDir);
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
                Uri photoURI = FileProvider.getUriForFile(this, "au.com.tfsltd.invertebrateKey.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                mCurrentPhotoUri = photoURI;
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void saveObservation(Observation observation) {
        DatabaseReference mFirebaseRef = database.getReference();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        assert auth.getCurrentUser() != null;
        String userUid = auth.getCurrentUser().getUid();

        DatabaseReference o = mFirebaseRef.child(Constants.FIELD_OBSERVATIONS).child(userUid).child(possibleAnswers).push();
        o.setValue(observation);
    }

    private void callShareActivity() {
        Intent intent = new Intent(DetailActivity.this, ShareActivity.class);
        intent.putExtra(Constants.PATH, Constants.FIELD_OBSERVATIONS + Constants.PATH_SEPARATOR + userId + Constants.PATH_SEPARATOR + possibleAnswers);
        startActivity(intent);
    }

}
