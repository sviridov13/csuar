package com.csu.ar;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class BackgroundListAdapter extends RecyclerView.Adapter<BackgroundListAdapter.ViewHolder> {

    private String TAG = "BackgroundListAdapter";

    private OnItemClickListener listener;

    private ArrayList<String> imageUrls;
    private int Height;
    private Context mContext;

    public BackgroundListAdapter(Context context, ArrayList<String> arrayList, int card_height) {
        mContext = context;
        imageUrls = arrayList;
        Height = card_height;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.background_list_items, viewGroup, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        if (i==0)
            viewHolder.image.setImageDrawable(mContext.getResources().getDrawable(R.drawable.default_pic));
        else
            viewHolder.image.setImageURI(Uri.parse(imageUrls.get(i)));
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        CardView cardView;
        RelativeLayout relativeLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            image = itemView.findViewById(R.id.back_image);
            cardView = itemView.findViewById(R.id.back_card);
            relativeLayout = itemView.findViewById(R.id.background_element);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(imageUrls.get(getAdapterPosition()));
                }
            });
        }
    }

    public interface OnItemClickListener {
        void onItemClick(String drawableUri);
    }

    public void setOnItemClickListener(BackgroundListAdapter.OnItemClickListener listener) {
        this.listener = listener;
    }

}
