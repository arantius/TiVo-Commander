package com.arantius.tivocommander;

import android.app.ListActivity;
import android.os.Bundle;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.PersonSearch;

public class Person extends ListActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    Bundle bundle = getIntent().getExtras();
    if (bundle == null) {
      finish();
      return;
    }

//    setContentView(R.layout.progress);
    String personId = bundle.getString("personId");
    setTitle(String.format("TiVo Commander - %s %s", bundle.getString("fName"),
        bundle.getString("lName")));
    MindRpc.addRequest(new PersonSearch(personId), null);
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
