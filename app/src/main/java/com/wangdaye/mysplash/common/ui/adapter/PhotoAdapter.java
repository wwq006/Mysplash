package com.wangdaye.mysplash.common.ui.adapter;

import android.content.Context;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wangdaye.mysplash.Mysplash;
import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash.common._basic.FooterAdapter;
import com.wangdaye.mysplash.common.data.entity.unsplash.ChangeCollectionPhotoResult;
import com.wangdaye.mysplash.common.data.entity.unsplash.LikePhotoResult;
import com.wangdaye.mysplash.common.data.entity.unsplash.Photo;
import com.wangdaye.mysplash.common.data.service.PhotoService;
import com.wangdaye.mysplash.common._basic.MysplashActivity;
import com.wangdaye.mysplash.common.ui.dialog.DownloadRepeatDialog;
import com.wangdaye.mysplash.common.ui.widget.CircularProgressIcon;
import com.wangdaye.mysplash.common.utils.DisplayUtils;
import com.wangdaye.mysplash.common.utils.FileUtils;
import com.wangdaye.mysplash.common.utils.helper.NotificationHelper;
import com.wangdaye.mysplash.common.utils.helper.DatabaseHelper;
import com.wangdaye.mysplash.common.utils.helper.ImageHelper;
import com.wangdaye.mysplash.common.utils.helper.IntentHelper;
import com.wangdaye.mysplash.common.utils.manager.AuthManager;
import com.wangdaye.mysplash.common.ui.dialog.DeleteCollectionPhotoDialog;
import com.wangdaye.mysplash.common.ui.dialog.SelectCollectionDialog;
import com.wangdaye.mysplash.common.ui.widget.freedomSizeView.FreedomImageView;
import com.wangdaye.mysplash.collection.view.activity.CollectionActivity;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Photo adapter.
 *
 * Adapter for {@link RecyclerView} to show photos.
 *
 * */

public class PhotoAdapter extends FooterAdapter<RecyclerView.ViewHolder>
        implements DeleteCollectionPhotoDialog.OnDeleteCollectionListener,
        DownloadRepeatDialog.OnCheckOrDownloadListener {
    // widget
    private Context a;
    private RecyclerView recyclerView;
    private SelectCollectionDialog.OnCollectionsChangedListener collectionsChangedListener;
    private OnDownloadPhotoListener downloadPhotoListener;

    // data
    private List<Photo> itemList;
    private PhotoService service;

    // if set true, it means these photos is in a collection that was created by user.
    private boolean inMyCollection = false;

    /** <br> life cycle. */

    public PhotoAdapter(Context a, List<Photo> list,
                        SelectCollectionDialog.OnCollectionsChangedListener sl,
                        OnDownloadPhotoListener dl) {
        this.a = a;
        this.itemList = list;
        this.collectionsChangedListener = sl;
        this.downloadPhotoListener = dl;
    }

    @Override
    protected boolean hasFooter() {
        return DisplayUtils.getNavigationBarHeight(a.getResources()) != 0;
    }

    @Override
    public int getRealItemCount() {
        return itemList.size();
    }

    /** <br> UI. */

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        if (isFooter(position)) {
            // footer.
            return FooterHolder.buildInstance(parent);
        } else {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_photo, parent, false);
            return new ViewHolder(v, position);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder && position < getRealItemCount()) {
            ((ViewHolder) holder).onBindView(position);
        }
    }

    public void setActivity(MysplashActivity a) {
        this.a = a;
    }

    public void setRecyclerView(RecyclerView v) {
        this.recyclerView = v;
    }

    /** <br> data. */

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof ViewHolder) {
            ((ViewHolder) holder).onRecycled();
        }
    }

    public void insertItem(Photo item) {
        if (item.width != 0 && item.height != 0) {
            itemList.add(item);
            notifyItemInserted(itemList.size() - 1);
        }
    }

    public void insertItemToFirst(Photo item) {
        if (item.width != 0 && item.height != 0) {
            itemList.add(0, item);
            notifyItemInserted(0);
        }
    }

    public void removeItem(Photo item) {
        for (int i = 0; i < itemList.size(); i ++) {
            if (itemList.get(i).id .equals(item.id)) {
                itemList.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public void clearItem() {
        itemList.clear();
        notifyDataSetChanged();
    }

    private void setLikeForAPhoto(boolean like, int position) {
        if (service == null) {
            service = PhotoService.getService();
        }
        itemList.get(position).settingLike = true;
        service.setLikeForAPhoto(
                itemList.get(position).id,
                like,
                new OnSetLikeListener(itemList.get(position).id, position));
    }

    public void setInMyCollection(boolean in) {
        this.inMyCollection = in;
    }

    public void updatePhoto(Photo p, boolean probablyRepeat) {
        for (int i = 0; i < getRealItemCount(); i ++) {
            if (itemList.get(i).id.equals(p.id)) {
                itemList.set(i, p);
                notifyItemChanged(i);
                if (!probablyRepeat) {
                    return;
                }
            }
        }
    }

    public void setPhotoData(List<Photo> list) {
        itemList.clear();
        itemList.addAll(list);
        notifyDataSetChanged();
    }

    public List<Photo> getPhotoData() {
        List<Photo> list = new ArrayList<>();
        list.addAll(itemList);
        return list;
    }

    /** <br> interface. */

    // on download photo listener.

    public interface OnDownloadPhotoListener {
        void onDownload(Photo photo);
    }

    public void setOnDownloadPhotoListener(OnDownloadPhotoListener l) {
        this.downloadPhotoListener = l;
    }

    // on set like listener.

    private class OnSetLikeListener implements PhotoService.OnSetLikeListener {
        // data
        private String id;
        private int position;

        // life cycle.

        OnSetLikeListener(String id, int position) {
            this.id = id;
            this.position = position;
        }

        // interface.

        @Override
        public void onSetLikeSuccess(Call<LikePhotoResult> call, Response<LikePhotoResult> response) {
            if (itemList.size() >= position
                    && itemList.get(position).id.equals(id)) {
                itemList.get(position).settingLike = false;

                if (response.isSuccessful() && response.body() != null) {
                    itemList.get(position).liked_by_user = response.body().photo.liked_by_user;
                    itemList.get(position).likes = response.body().photo.likes;
                }

                updateView(itemList.get(position).liked_by_user);
            }
        }

        @Override
        public void onSetLikeFailed(Call<LikePhotoResult> call, Throwable t) {
            if (itemList.size() >= position
                    && itemList.get(position).id.equals(id)) {
                itemList.get(position).settingLike = false;
                updateView(itemList.get(position).liked_by_user);
                NotificationHelper.showSnackbar(
                        itemList.get(position).liked_by_user ?
                                a.getString(R.string.feedback_unlike_failed)
                                :
                                a.getString(R.string.feedback_like_failed),
                        Snackbar.LENGTH_SHORT);
            }
        }

        // UI.

        private void updateView(boolean to) {
            if (recyclerView != null) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                int firstPosition = layoutManager.findFirstVisibleItemPosition();
                int lastPosition = layoutManager.findLastVisibleItemPosition();
                if (firstPosition <= position && position <= lastPosition) {
                    ViewHolder holder = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
                    holder.likeButton.setResultState(
                            to ? R.drawable.ic_item_heart_red : R.drawable.ic_item_heart_outline);
                }
            }
        }
    }

    // on delete collection photo listener.

    @Override
    public void onDeletePhotoSuccess(ChangeCollectionPhotoResult result, int position) {
        if (itemList.size() > position && itemList.get(position).id.equals(result.photo.id)) {
            itemList.remove(position);
            notifyItemRemoved(position);
        }
    }

    // on check or download listener. (download repeat dialog)

    @Override
    public void onCheck(Object obj) {
        IntentHelper.startCheckPhotoActivity(a, ((Photo) obj).id);
    }

    @Override
    public void onDownload(Object obj) {
        if (downloadPhotoListener != null) {
            downloadPhotoListener.onDownload((Photo) obj);
        }
    }

    /** <br> inner class. */

    // view holder.

    class ViewHolder extends RecyclerView.ViewHolder {
        // widget
        @BindView(R.id.item_photo_background) RelativeLayout background;
        @BindView(R.id.item_photo_image) FreedomImageView image;
        @BindView(R.id.item_photo_title) TextView title;
        @BindView(R.id.item_photo_deleteButton) ImageButton deleteButton;
        @BindView(R.id.item_photo_collectionButton) ImageButton collectionButton;
        @BindView(R.id.item_photo_likeButton) CircularProgressIcon likeButton;

        // life cycle.

        ViewHolder(View itemView, int position) {
            super(itemView);
            ButterKnife.bind(this, itemView);

            image.setSize(itemList.get(position).width, itemList.get(position).height);
            DisplayUtils.setTypeface(itemView.getContext(), title);
        }

        // UI.

        void onBindView(final int position) {
            title.setText("");
            image.setShowShadow(false);

            ImageHelper.loadRegularPhoto(a, image, itemList.get(position), new ImageHelper.OnLoadImageListener() {
                @Override
                public void onLoadSucceed() {
                    itemList.get(position).loadPhotoSuccess = true;
                    if (!itemList.get(position).hasFadedIn) {
                        itemList.get(position).hasFadedIn = true;
                        ImageHelper.startSaturationAnimation(a, image);
                    }
                    title.setText(itemList.get(position).user.name);
                    image.setShowShadow(true);
                }

                @Override
                public void onLoadFailed() {
                    // do nothing.
                }
            });

            if (inMyCollection) {
                deleteButton.setVisibility(View.VISIBLE);
            } else {
                deleteButton.setVisibility(View.GONE);
            }
            if (itemList.get(position).current_user_collections.size() != 0) {
                collectionButton.setImageResource(R.drawable.ic_item_collected);
            } else {
                collectionButton.setImageResource(R.drawable.ic_item_collect);
            }

            if (itemList.get(position).settingLike) {
                likeButton.forceSetProgressState();
            } else {
                likeButton.forceSetResultState(itemList.get(position).liked_by_user ?
                        R.drawable.ic_item_heart_red : R.drawable.ic_item_heart_outline);
            }

            background.setBackgroundColor(
                    ImageHelper.computeCardBackgroundColor(
                            a,
                            itemList.get(position).color));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                image.setTransitionName(itemList.get(position).id + "-image");
                background.setTransitionName(itemList.get(position).id + "-background");
            }
        }

        void onRecycled() {
            ImageHelper.releaseImageView(image);
            likeButton.recycleImageView();
        }

        // interface.

        @OnClick(R.id.item_photo_background) void clickItem() {
            if (a instanceof MysplashActivity) {
                IntentHelper.startPhotoActivity(
                        (MysplashActivity) a,
                        image,
                        background,
                        itemList.get(getAdapterPosition()));
            }
        }

        @OnClick(R.id.item_photo_deleteButton) void deletePhoto() {
            if (a instanceof CollectionActivity) {
                DeleteCollectionPhotoDialog dialog = new DeleteCollectionPhotoDialog();
                dialog.setDeleteInfo(
                        ((CollectionActivity) a).getCollection(),
                        itemList.get(getAdapterPosition()),
                        getAdapterPosition());
                dialog.setOnDeleteCollectionListener(PhotoAdapter.this);
                dialog.show(((CollectionActivity) a).getFragmentManager(), null);
            }
        }

        @OnClick(R.id.item_photo_likeButton) void likePhoto() {
            if (AuthManager.getInstance().isAuthorized()) {
                if (likeButton.isUsable()) {
                    likeButton.setProgressState();
                    setLikeForAPhoto(
                            !itemList.get(getAdapterPosition()).liked_by_user,
                            getAdapterPosition());
                }
            } else {
                IntentHelper.startLoginActivity((MysplashActivity) a);
            }
        }

        @OnClick(R.id.item_photo_collectionButton) void collectPhoto() {
            if (a instanceof MysplashActivity) {
                if (!AuthManager.getInstance().isAuthorized()) {
                    IntentHelper.startLoginActivity((MysplashActivity) a);
                } else {
                    SelectCollectionDialog dialog = new SelectCollectionDialog();
                    dialog.setPhotoAndListener(itemList.get(getAdapterPosition()), collectionsChangedListener);
                    dialog.show(((MysplashActivity) a).getFragmentManager(), null);
                }
            }
        }

        @OnClick(R.id.item_photo_downloadButton) void downloadPhoto() {
            Photo p = itemList.get(getAdapterPosition());
            if (DatabaseHelper.getInstance(a).readDownloadingEntityCount(p.id) > 0) {
                NotificationHelper.showSnackbar(
                        a.getString(R.string.feedback_download_repeat),
                        Snackbar.LENGTH_SHORT);
            } else if (FileUtils.isPhotoExists(a, p.id)) {
                MysplashActivity activity = Mysplash.getInstance().getTopActivity();
                if (activity != null) {
                    DownloadRepeatDialog dialog = new DownloadRepeatDialog();
                    dialog.setDownloadKey(p);
                    dialog.setOnCheckOrDownloadListener(PhotoAdapter.this);
                    dialog.show(activity.getFragmentManager(), null);
                }
            } else {
                if (downloadPhotoListener != null) {
                    downloadPhotoListener.onDownload(p);
                }
            }
        }
    }
}
