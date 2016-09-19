package au.com.tfsltd.tfs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Detail activity
 *
 * Created by adrian on 17.9.2016.
 */
public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String path = getIntent().getExtras().getString(Constants.PATH);
        assert path != null;

        setContentView(R.layout.activity_detail);

        final TextView detailView = (TextView) findViewById(R.id.detail);
        final LinearLayout photosLayout = (LinearLayout) findViewById(R.id.photos);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mFirebaseRef = database.getReference(path);

        mFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map detail = (Map)dataSnapshot.getValue();

                detailView.setText((String)detail.get(Constants.FIELD_DETAIL));

                List<String> photos = (List<String>)detail.get(Constants.FIELD_PHOTOS);
                for(String photoUrl : photos) {
                    photosLayout.addView(createPhotoImage(photoUrl));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getMessage());
            }
        });
    }

    private ImageView createPhotoImage(final String photoPath) {
        ImageView photo = new ImageView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(20, 10, 20, 10);
        photo.setLayoutParams(lp);
        photo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        photo.setAdjustViewBounds(true);

        File imgFile = new File(this.getApplicationContext().getFilesDir() + "/" + photoPath + ".jpg");

        if(imgFile.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            photo.setImageBitmap(myBitmap);
        }

        return photo;
    }


}
