package com.example.sca.History.RecyclerViewMatierials;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sca.History.HistorySingleActivity;
import com.example.sca.R;

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView deliveryID;
    public TextView timeID;
    public HistoryViewHolders(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);

        deliveryID = itemView.findViewById(R.id.tvDeliveryID);
        timeID = itemView.findViewById(R.id.tvTime);
    }

    @Override
    public void onClick(View view) {
        Intent eye = new Intent(view.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("deliveryID", deliveryID.getText().toString());
        eye.putExtras(b);
        view.getContext().startActivity(eye);
    }
}
