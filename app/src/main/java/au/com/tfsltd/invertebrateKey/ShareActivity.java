package au.com.tfsltd.invertebrateKey;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * ShareActivity - share photos and location
 *
 * Created by adrian on 9.10.2016.
 */

public class ShareActivity extends AppCompatActivity {

    private String path;
    private List<Observation> observations;
    private List<Boolean> checks;

    private SimpleDateFormat sdf = new SimpleDateFormat();

    FloatingActionButton fabMail;

    public class ObservationAdapter extends RecyclerView.Adapter<ObservationAdapter.ObservationHolder> {

        class ObservationHolder extends RecyclerView.ViewHolder {
            CheckBox checkBox;
            TextView date;
            Button location;
            ImageView photo;

            ObservationHolder(View v) {
                super(v);

                checkBox = (CheckBox) v.findViewById(R.id.observation_check);
                date = (TextView) v.findViewById(R.id.observation_date);
                location = (Button) v.findViewById(R.id.observation_location);
                photo = (ImageView) v.findViewById(R.id.observation_photo);
            }
        }

        ObservationAdapter(DatabaseReference ref) {
            observations = new ArrayList<>();
            checks = new ArrayList<>();
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    observations.clear();
                    for (DataSnapshot postSnapshot: snapshot.getChildren()) {
                        Observation observation = postSnapshot.getValue(Observation.class);
                        observations.add(0, observation);
                        checks.add(0, !observation.isSent());
                    }
                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        @Override
        public ObservationHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ObservationHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.observation, parent, false));
        }

        @Override
        public void onBindViewHolder(final ObservationHolder holder, int position) {
            final Observation observation = observations.get(position);

            holder.checkBox.setChecked(checks.get(position));
            holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    checks.set(holder.getAdapterPosition(), compoundButton.isChecked());
                }
            });
            holder.date.setText(sdf.format(observation.getDate()));
            holder.location.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ShareActivity.this, WebViewActivity.class);
                    intent.putExtra(Constants.MAP_LINK, getMapLink(observation.getLatitude(), observation.getLongitude()));
                    startActivity(intent);
                }
            });

            DisplayMetrics dm = getResources().getDisplayMetrics();
            int size = (int) ((dm.widthPixels - 2 * getResources().getDimension(R.dimen.card_view_margin) - 4 * getResources().getDimension(R.dimen.answer_margin)) / 2);
            holder.photo.getLayoutParams().width = size;
            holder.photo.getLayoutParams().height = size;

            Bitmap myBitmap = ((TFSApp)getApplication()).getBitmapFromMemCache(observation.getPhotoPath());
            if (myBitmap == null) {

                File imgFile = getExternalFilesDir(Environment.DIRECTORY_PICTURES + observation.getPhotoPath());
                if (imgFile != null && imgFile.exists()) {
                    myBitmap = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(imgFile.getAbsolutePath()),
                            Constants.THUMBNAIL_SIZE, Constants.THUMBNAIL_SIZE);
                    ((TFSApp) getApplication()).addBitmapToMemoryCache(observation.getPhotoPath(), myBitmap);
                    holder.photo.setImageBitmap(myBitmap);
                }
            } else {
                holder.photo.setImageBitmap(myBitmap);
            }
        }

        @Override
        public int getItemCount() {
            return observations.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getIntent().getExtras().getString(Constants.PATH);
        assert(path != null);

        setContentView(R.layout.activity_share);

        fabMail = (FloatingActionButton) findViewById(R.id.fab_mail);
        fabMail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int i = 0;
                List<Observation> emailObservations = new ArrayList<Observation>();
                for(Observation observation : observations) {
                    if (checks.get(i)) {
                        emailObservations.add(observation);
                    }
                }
                dispatchMailIntent(emailObservations);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        RecyclerView observationList = (RecyclerView) findViewById(R.id.observation_list);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        observationList.setLayoutManager(layoutManager);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        ObservationAdapter observationAdapter = new ObservationAdapter(database.getReference(path));
        observationList.setAdapter(observationAdapter);
    }

    private String getMapLink(double latitude, double longitude) {
        return Constants.GOOGLE_MAPS + latitude + "," + longitude + "/@" + latitude + "," + longitude + "," + Constants.ZOOM;

    }

    private void dispatchMailIntent(List<Observation> observations) {
        String subject = this.getResources().getString(R.string.email_subject);

        final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{Constants.EMAIL});
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

        final StringBuilder text = new StringBuilder();
        ArrayList<Uri> uris = new ArrayList<>();
        for (Observation observation : observations) {

            text.append(observation.getPhotoPath().substring(observation.getPhotoPath().lastIndexOf(Constants.PATH_SEPARATOR)));
            text.append("<br/>");
            text.append(sdf.format(observation.getDate()));
            text.append("<br/>");
            text.append("<a href=\"");
            text.append(getMapLink(observation.getLatitude(), observation.getLongitude()));
            text.append("\">");
            text.append(getResources().getString(R.string.map));
            text.append("</a>");
            text.append("<br/><br/>");

            File file = getExternalFilesDir(Environment.DIRECTORY_PICTURES + observation.getPhotoPath());
            if (!file.exists() || !file.canRead()) {
                Toast.makeText(this, "Attachment Error", Toast.LENGTH_SHORT).show();
            } else {
                Uri uri = Uri.fromFile(file);
                uris.add(uri);
            }
        }
        Spanned result;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(text.toString(), Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(text.toString());
        }

        emailIntent.putExtra(Intent.EXTRA_TEXT, result);
        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        startActivity(Intent.createChooser(emailIntent, this.getResources().getString(R.string.email_sending)));
    }

}
