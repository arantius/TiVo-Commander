package com.arantius.tivocommander;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jackson.JsonNode;

import android.app.ListActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.PersonSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Person extends ListActivity {
  private final MindRpcResponseListener mPersonListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          setContentView(R.layout.list_person);
          JsonNode person = response.getBody().path("person").path(0);

          // Name.
          ((TextView) findViewById(R.id.person_name)).setText(mName);

          // Role.
          JsonNode rolesNode = person.path("roleForPersonId");
          String[] roles = new String[rolesNode.size()];
          for (int i = 0; i < rolesNode.size(); i++) {
            roles[i] = rolesNode.path(i).getTextValue();
            roles[i] =
                roles[i].substring(0, 1).toUpperCase() + roles[i].substring(1);
          }
          ((TextView) findViewById(R.id.person_role)).setText(Utils.join(
              ", ", roles));

          // Birthdate.
          SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-mm-dd");
          ParsePosition pp = new ParsePosition(0);
          Date birthdate =
              dateParser.parse(person.path("birthDate").getTextValue(), pp);
          SimpleDateFormat dateFormatter =
              new SimpleDateFormat("MMMMM d, yyyy");
          Spannable birthdateStr =
              new SpannableString("Birthdate: "
                  + dateFormatter.format(birthdate));
          birthdateStr.setSpan(new ForegroundColorSpan(Color.WHITE), 11,
              birthdateStr.length(), 0);
          ((TextView) findViewById(R.id.person_birthdate))
              .setText(birthdateStr);

          Spannable birthplaceStr =
              new SpannableString("Birthplace: "
                  + person.path("birthPlace").getTextValue());
          birthplaceStr.setSpan(new ForegroundColorSpan(Color.WHITE), 12,
              birthplaceStr.length(), 0);
          ((TextView) findViewById(R.id.person_birthplace))
              .setText(birthplaceStr);

          ImageView iv = (ImageView) findViewById(R.id.imageView1);
          View pv = findViewById(R.id.progressBar1);
          String imgUrl = Utils.findImageUrl(person);
          new DownloadImageTask(iv, pv).execute(imgUrl);
        }
      };
  private String mName;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    Bundle bundle = getIntent().getExtras();
    if (bundle == null) {
      finish();
      return;
    }

    String personId = bundle.getString("personId");
    mName =
        String.format("%s %s", bundle.getString("fName"),
            bundle.getString("lName"));
    setTitle("TiVo Commander - " + mName);
    MindRpc.addRequest(new PersonSearch(personId), mPersonListener);
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
