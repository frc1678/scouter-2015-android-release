package org.citruscircuits.scout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

public class AutoMatchActivity extends Activity {
    public int backToMenu = 0;
    public List<Map<String, Object>> reconAcquisitions = new ArrayList<Map<String, Object>>();
    private int middleRemainingRecons = 2;
    private int sideRemainingRecons = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto);

        Intent intent = getIntent();

        TextView teamTextView = (TextView) findViewById(R.id.teamLabelAuto);
        if (intent.getBooleanExtra("allianceColor", false)) {
            teamTextView.setTextColor(Color.RED);
        } else {
            teamTextView.setTextColor(Color.BLUE);
        }

        teamTextView.setText(((Integer) intent.getIntExtra("teamNumber", 0))
                .toString());
        String matchNumber = intent.getStringExtra("matchNumber");

        setTitle(matchNumber);

        setupAutoButtons();
    }

    int reconCounter = 0;

    public void reconsAutoMinus(View view) {
        if (reconCounter > -7) {
            reconCounter--;
            TextView x = (TextView) findViewById(R.id.reconsIntoAuto);
            x.setText(Integer.toString(reconCounter));
        }
    }

    public void reconsAutoPlus(View view) {
        if (reconCounter < 7) {
            reconCounter++;
            TextView x = (TextView) findViewById(R.id.reconsIntoAuto);
            x.setText(Integer.toString(reconCounter));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.auto_match, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items

        if (item.getItemId() == R.id.action_next) {
            Intent teleIntent = new Intent(this, TeleMatchActivity.class);
            Intent oldIntent = getIntent();

            teleIntent.putExtra("teamNumber",
                    oldIntent.getIntExtra("teamNumber", 0));

            Log.e("test", "The team number is " + oldIntent.getIntExtra("teamNumber", 0));

            teleIntent.putExtra("matchNumber",
                    oldIntent.getStringExtra("matchNumber"));
            teleIntent.putExtra("channel", oldIntent.getIntExtra("channel", 5));
            teleIntent.putExtra("allianceColor",
                    oldIntent.getBooleanExtra("allianceColor", true));
            teleIntent.putExtra("scoutName",
                    oldIntent.getStringExtra("scoutName"));

            Map<String, Object> data = new HashMap<String, Object>();
            Integer reconsIntoAuto = Integer.parseInt(((TextView) findViewById(R.id.reconsIntoAuto)).getText().toString());

            data.put("numContainersMovedIntoAutoZone", reconsIntoAuto);
            data.put("stackedToteSet", ((ToggleButton)findViewById(R.id.stackedToteSetInAuto)).isChecked());
            data.put("reconAcquisitions", reconAcquisitions);

            ArrayList<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
            dataList.add(data);

            teleIntent.putExtra("data", dataList);
            teleIntent.putExtra("reconsFailed", ((TextView)findViewById(R.id.reconFailNum)).getText());

            startActivityForResult(teleIntent, backToMenu);
        }
        return true;
    }

    public void reconFailedClick(View view) {

        TextView numView = ((TextView)(findViewById(R.id.reconFailNum)));

        int num = 0;
        if (view.getTag().toString().substring(1,2).equals("+")) {
            num = Integer.parseInt((String)numView.getText())+1;
        } else {
            num = Integer.parseInt((String)numView.getText())-1;
        }
        if (num<0) {
            num = 0;
        }
        numView.setText(Integer.toString(num));
    }


    public void setupAutoButtons() {
        Button oneMButton = (Button) findViewById(R.id.oneMButton);
        Button oneSButton = (Button) findViewById(R.id.oneSButton);
        Button twoMButton = (Button) findViewById(R.id.twoMButton);
        Button twoSButton = (Button) findViewById(R.id.twoSButton);
        Button threeButton = (Button) findViewById(R.id.threeButton);
        Button fourButton = (Button) findViewById(R.id.fourButton);

        oneMButton.setOnClickListener(reconListener);
        oneSButton.setOnClickListener(reconListener);
        twoMButton.setOnClickListener(reconListener);
        twoSButton.setOnClickListener(reconListener);
        threeButton.setOnClickListener(reconListener);
        fourButton.setOnClickListener(reconListener);
    }

    private View.OnClickListener reconListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Map<String, Object> reconAcquisition = new HashMap<String, Object>();
            String buttonText = ((Button) view).getText().toString();

            int numRecons = Integer.parseInt(buttonText.substring(0, 1));
            reconAcquisition.put("numReconsAcquired", numRecons);

            Log.e("test", "hello");
            Log.e("test", buttonText.substring(buttonText.length() - 4, buttonText.length()));

            boolean side;
            if (buttonText.substring(buttonText.length() - 4, buttonText.length()).equals("Side")) {
                Log.e("test", "True");
                side = true;
            } else {
                side = false;
            }
            reconAcquisition.put("acquiredMiddle", !side);

            if (side || numRecons == 3) {
                int middle = numRecons - 1;
                Log.e("test", "The number of middle recons taken is " + middle);
                sideRemainingRecons--;
                middleRemainingRecons -= middle;
            } else if (numRecons == 4) {
                middleRemainingRecons = 0;
                sideRemainingRecons = 0;
            } else {
                middleRemainingRecons -= numRecons;
            }

            Log.e("test", "The remaining middle recons is " + middleRemainingRecons);
            Log.e("test", "The remaining side recons is " + sideRemainingRecons);


            List<Button> buttons = new ArrayList<Button>();

            Button oneMButton = (Button) findViewById(R.id.oneMButton);
            Button oneSButton = (Button) findViewById(R.id.oneSButton);
            Button twoMButton = (Button) findViewById(R.id.twoMButton);
            Button twoSButton = (Button) findViewById(R.id.twoSButton);
            Button threeButton = (Button) findViewById(R.id.threeButton);
            Button fourButton = (Button) findViewById(R.id.fourButton);

            buttons.add(oneMButton);
            buttons.add(oneSButton);
            buttons.add(twoMButton);
            buttons.add(twoSButton);
            buttons.add(threeButton);
            buttons.add(fourButton);

            for (Button button : buttons) {
                String forButtonText = button.getText().toString();
                int forButtonNumRecons = Integer.parseInt(forButtonText.substring(0, 1));
                Log.e("test", forButtonNumRecons + "");
                boolean isSide = (forButtonText.substring(forButtonText.length() - 4, forButtonText.length()).equals("Side") || forButtonNumRecons == 3);
                if (forButtonNumRecons > middleRemainingRecons + sideRemainingRecons) {
                    button.setEnabled(false);
                } else if (isSide && sideRemainingRecons < 1 || (forButtonNumRecons - 1) > middleRemainingRecons) {
                    button.setEnabled(false);
                } else if (!isSide && forButtonNumRecons > middleRemainingRecons) {
                    button.setEnabled(false);
                }
            }
            reconAcquisitions.add(reconAcquisition);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == backToMenu) {
            if (resultCode == RESULT_OK) {
                setResult(42, data);
                finish();
            }
        }
        if (resultCode == 666) {
            setResult(666);
            Log.e("dsffdsfdsfdsfsdfsdfsdf", "THING!");
            finish();
        }
    }
}
