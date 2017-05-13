package com.distancelin.jjlin;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private String[] mUrls = {"http://09.imgmini.eastday.com/mobile/20170512/20170512171222_361767009a24c1bfaae49ceb1fd725f6_2_mwpm_03200403.jpeg",
            "http://09.imgmini.eastday.com/mobile/20170512/20170512171222_361767009a24c1bfaae49ceb1fd725f6_3_mwpm_03200403.jpeg",
            "http://09.imgmini.eastday.com/mobile/20170512/20170512171222_361767009a24c1bfaae49ceb1fd725f6_1_mwpm_03200403.jpeg",
            "http://00.imgmini.eastday.com/mobile/20170512/20170512171121_948a6eba7005b8096be06d5f188afd82_1_mwpm_03200403.png",
            "http://00.imgmini.eastday.com/mobile/20170512/20170512171121_948a6eba7005b8096be06d5f188afd82_4_mwpm_03200403.png",
            "http://06.imgmini.eastday.com/mobile/20170512/20170512170926_160574a0d29934e86694b38a0caff86c_3_mwpm_03200403.jpeg",
            "http://06.imgmini.eastday.com/mobile/20170512/20170512170926_160574a0d29934e86694b38a0caff86c_1_mwpm_03200403.jpeg",
            "http://06.imgmini.eastday.com/mobile/20170512/20170512170926_160574a0d29934e86694b38a0caff86c_4_mwpm_03200403.jpeg",
            "http://00.imgmini.eastday.com/mobile/20170512/20170512170903_38e794d04b22805851b1b5ba923da663_5_mwpm_03200403.jpeg",
            "http://00.imgmini.eastday.com/mobile/20170512/20170512170903_38e794d04b22805851b1b5ba923da663_4_mwpm_03200403.jpeg",
            "http://01.imgmini.eastday.com/mobile/20170512/20170512170901_84fa7f4444ae30bf48c8ca5d580d5bea_1_mwpm_03200403.jpeg",
            "http://05.imgmini.eastday.com/mobile/20170512/20170512170830_407fcb197f95941bfd6c860171f6e2d2_1_mwpm_03200403.jpeg",
            "http://03.imgmini.eastday.com/mobile/20170512/20170512_ace5b9230a206621e48ca81a28e59373_cover_mwpm_03200403.png",
            "http://03.imgmini.eastday.com/mobile/20170512/20170512_9bd3289c2e2ac32d60278de4293f150a_cover_mwpm_03200403.jpeg",
            "http://03.imgmini.eastday.com/mobile/20170512/20170512_c3cbab2bb373ccfa4e2aac5dcb979169_cover_mwpm_03200403.png",
            "http://00.imgmini.eastday.com/mobile/20170512/20170512170055_0899a0c22bc92ad385b5349deefece9a_3_mwpm_03200403.jpeg",
            "http://00.imgmini.eastday.com/mobile/20170512/20170512170055_0899a0c22bc92ad385b5349deefece9a_1_mwpm_03200403.jpeg",
            "http://00.imgmini.eastday.com/mobile/20170512/20170512170055_0899a0c22bc92ad385b5349deefece9a_2_mwpm_03200403.jpeg",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button mButton = (Button) findViewById(R.id.download);
        final GridView gridView = (GridView) findViewById(R.id.grid);
        final Adapter adapter = new Adapter(this, Arrays.asList(mUrls));
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gridView.setAdapter(adapter);
            }
        });
    }
}