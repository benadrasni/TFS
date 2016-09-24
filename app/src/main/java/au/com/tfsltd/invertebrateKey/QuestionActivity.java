package au.com.tfsltd.invertebrateKey;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.util.Map;

/**
 * Question activity
 *
 * Created by adrian on 17.9.2016.
 */
public class QuestionActivity extends AppCompatActivity {

    private String path;
    //private Bitmap[] myBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getIntent().getExtras().getString(Constants.PATH);

        setContentView(R.layout.activity_question);

        final TextView questionView = (TextView) findViewById(R.id.question);
        final LinearLayout answersView = (LinearLayout) findViewById(R.id.answers);
        final GridLayout answersGridView = (GridLayout) findViewById(R.id.answers_grid);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mFirebaseRef = database.getReference(path);

        mFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map question = (Map)dataSnapshot.getValue();

                questionView.setText((String)question.get(Constants.FIELD_TEXT));

                Map answers = (Map)question.get(Constants.FIELD_ANSWERS);
                //myBitmap = new Bitmap[answers.size()];
                for(int i = 1; i <= answers.size(); i++) {
                    final Map answer = (Map) answers.get(Constants.FIELD_ANSWER_PREFIX + i);

                    if (i == answers.size() && answers.size() % 2 == 1) {
                        answersView.addView(createAnswerLayout(answer, i, false));
                    } else {
                        answersGridView.addView(createAnswerLayout(answer, i, true));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getMessage());
            }
        });
    }

//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        for (Bitmap bitmap : myBitmap) {
//            if (bitmap != null) {
//                bitmap.recycle();
//            }
//        }
//    }

    private LinearLayout createAnswerLayout(final Map answer, final int i, boolean isInGrid) {
        final LinearLayout answerLayout = (LinearLayout) LayoutInflater.from(this.getApplicationContext()).inflate(R.layout.button_answer, null);
        TextView textView = (TextView) answerLayout.findViewById(R.id.answer_text);
        textView.setText((String)answer.get(Constants.FIELD_TEXT));

        ImageView photoView = (ImageView) answerLayout.findViewById(R.id.answer_photo);
        DisplayMetrics dm = this.getResources().getDisplayMetrics();
        if (isInGrid) {
            int size = (int) ((dm.widthPixels - 2 * this.getResources().getDimension(R.dimen.card_view_margin)
                    - 4 * this.getResources().getDimension(R.dimen.answer_margin)) / 2);
            photoView.getLayoutParams().width = size;
            photoView.getLayoutParams().height = size;
        } else {
            int size = (int) (dm.widthPixels - 2 * this.getResources().getDimension(R.dimen.card_view_margin)
                    - 2 * this.getResources().getDimension(R.dimen.answer_margin));
            photoView.getLayoutParams().width = size;
            photoView.getLayoutParams().height = size / 4;
        }

        String imageKey = Constants.PATH_SEPARATOR + Constants.ANSWER_DIR + Constants.PATH_SEPARATOR + answer.get(Constants.FIELD_PHOTO);
        Bitmap myBitmap = ((TFSApp)getApplication()).getBitmapFromMemCache(imageKey);
        if (myBitmap == null) {
            File imgFile = new File(this.getApplicationContext().getFilesDir() + imageKey);
            if (imgFile.exists()) {
                myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                ((TFSApp) getApplication()).addBitmapToMemoryCache(imageKey, myBitmap);
                photoView.setImageBitmap(myBitmap);
            }
        } else {
            photoView.setImageBitmap(myBitmap);
        }

//        File imgFile = new File(this.getApplicationContext().getFilesDir() + imageKey);
//
//        if (imgFile.exists()) {
//            myBitmap[i-1] = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
//            photoView.setImageBitmap(myBitmap[i-1]);
//        }

        answerLayout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Map subQuestion = (Map)answer.get(Constants.FIELD_QUESTION);
                if (subQuestion != null) {
                    Intent intent = new Intent(QuestionActivity.this, QuestionActivity.class);
                    intent.putExtra(Constants.PATH, path + Constants.PATH_SEPARATOR + Constants.FIELD_ANSWERS + Constants.PATH_SEPARATOR
                            + Constants.FIELD_ANSWER_PREFIX + i + Constants.PATH_SEPARATOR + Constants.FIELD_QUESTION);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(QuestionActivity.this, DetailActivity.class);
                    intent.putExtra(Constants.PATH, path + Constants.PATH_SEPARATOR + Constants.FIELD_ANSWERS + Constants.PATH_SEPARATOR
                            + Constants.FIELD_ANSWER_PREFIX + i);
                    startActivity(intent);
                }
            }
        });

        return answerLayout;
    }
}
