package au.com.tfsltd.tfs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

/**
 * Question activity
 *
 * Created by adrian on 17.9.2016.
 */
public class QuestionActivity extends AppCompatActivity {

    private String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getIntent().getExtras().getString(Constants.PATH);

        setContentView(R.layout.activity_question);

        final TextView questionView = (TextView) findViewById(R.id.question);
        final LinearLayout answersView = (LinearLayout) findViewById(R.id.answers);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mFirebaseRef = database.getReference(path);

        mFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map question = (Map)dataSnapshot.getValue();

                questionView.setText((String)question.get(Constants.FIELD_TEXT));

                Map answers = (Map)question.get(Constants.FIELD_ANSWERS);
                for(int i = 1; i <= answers.size(); i++) {
                    final Map answer = (Map) answers.get(Constants.FIELD_ANSWER_PREFIX + i);

                    answersView.addView(createAnswerButton(answer, i));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getMessage());
            }
        });
    }

    private Button createAnswerButton(final Map answer, final int i) {
        Button button = new Button(this);
        button.setLayoutParams(new LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT));
        button.setTransformationMethod(null);
        button.setText((String)answer.get(Constants.FIELD_TEXT));
        button.setOnClickListener(new View.OnClickListener() {
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

        return button;
    }
}
