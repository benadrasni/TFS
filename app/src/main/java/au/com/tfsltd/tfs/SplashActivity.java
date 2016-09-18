package au.com.tfsltd.tfs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.firebase.client.Firebase;

/**
 * Splash screen activity
 *
 * Created by adrian on 17.9.2016.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Firebase.setAndroidContext(this);
        Firebase.getDefaultConfig().setPersistenceEnabled(true);

        Intent intent = new Intent(this, QuestionActivity.class);
        intent.putExtra(Constants.PATH, Constants.FIELD_QUESTION);
        startActivity(intent);
        finish();
    }
}