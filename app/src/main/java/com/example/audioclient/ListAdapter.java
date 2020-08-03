package com.example.audioclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.MyViewHolder> {

    private Context mContext;
    private ArrayList<String> itemList;
    private ArrayList<String> durationList;

    private static int lastClickedPosition = -1;
    private int selectedItem;

    public ListAdapter(Context mContext, ArrayList<String> itemList, ArrayList<String> durationList){
        this.mContext = mContext;
        this.itemList = itemList;
        this.durationList = durationList;
        this.selectedItem = -1;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){

        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);

        final MyViewHolder viewHolder = new MyViewHolder(view);


        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position){
        holder.itemName.setText(itemList.get(position));
        holder.itemDuration.setText(durationList.get(position));
        holder.itemPosition.setText(String.valueOf(position+1)+".");
        holder.card.setBackgroundColor(mContext.getResources().getColor(R.color.unselected_item));

        if(selectedItem == position){
            holder.card.setBackgroundColor(mContext.getResources().getColor(R.color.selected_item));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int previousItem = selectedItem;
                selectedItem = position;

                ((ClipActivity) mContext).setClip(position);

                notifyItemChanged(previousItem);
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount(){
        return itemList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView itemName;
        TextView itemPosition;
        TextView itemDuration;
        RelativeLayout card;

        public MyViewHolder(@NonNull View itemView){
            super(itemView);

            itemName = (TextView) itemView.findViewById(R.id.item_name);
            itemPosition = (TextView) itemView.findViewById(R.id.item_num);
            itemDuration = (TextView) itemView.findViewById(R.id.duration);
            card = (RelativeLayout) itemView.findViewById(R.id.card);

        }
    }
}
