package com.github.anrimian.musicplayer.ui.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import com.github.anrimian.musicplayer.R;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.models.play_queue.PlayQueueItem;
import com.github.anrimian.musicplayer.domain.models.player.service.MusicNotificationSetting;
import com.github.anrimian.musicplayer.infrastructure.service.music.MusicService;
import com.github.anrimian.musicplayer.ui.common.images.CoverImageLoader;
import com.github.anrimian.musicplayer.ui.main.MainActivity;
import com.github.anrimian.musicplayer.ui.notifications.builder.AppNotificationBuilder;

import static androidx.core.content.ContextCompat.getColor;
import static com.github.anrimian.musicplayer.Constants.Actions.PAUSE;
import static com.github.anrimian.musicplayer.Constants.Actions.PLAY;
import static com.github.anrimian.musicplayer.Constants.Actions.SKIP_TO_NEXT;
import static com.github.anrimian.musicplayer.Constants.Actions.SKIP_TO_PREVIOUS;
import static com.github.anrimian.musicplayer.Constants.Arguments.OPEN_PLAY_QUEUE_ARG;
import static com.github.anrimian.musicplayer.domain.models.utils.CompositionHelper.formatCompositionName;
import static com.github.anrimian.musicplayer.infrastructure.service.music.MusicService.REQUEST_CODE;
import static com.github.anrimian.musicplayer.ui.common.format.FormatUtils.formatCompositionAuthor;


/**
 * Created on 05.11.2017.
 */

public class NotificationsDisplayer {

    public static final int FOREGROUND_NOTIFICATION_ID = 1;
    public static final int ERROR_NOTIFICATION_ID = 2;

    public static final String FOREGROUND_CHANNEL_ID = "0";
    public static final String ERROR_CHANNEL_ID = "1";

    private final Context context;
    private final NotificationManager notificationManager;
    private final AppNotificationBuilder notificationBuilder;
    private final CoverImageLoader coverImageLoader;

    private Bitmap currentNotificationBitmap;
    private Runnable cancellationRunnable;

    public NotificationsDisplayer(Context context,
                                  AppNotificationBuilder notificationBuilder,
                                  CoverImageLoader coverImageLoader) {
        this.context = context;
        this.notificationBuilder = notificationBuilder;
        this.coverImageLoader = coverImageLoader;

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(FOREGROUND_CHANNEL_ID,
                    getString(R.string.foreground_channel_description),
                    NotificationManager.IMPORTANCE_LOW);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);

            NotificationChannel errorChannel = new NotificationChannel(ERROR_CHANNEL_ID,
                    getString(R.string.error_channel_description),
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(errorChannel);
        }
    }

    public void showErrorNotification(@StringRes int errorMessageId) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        Notification notification = new NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.playing_error))
                .setContentText(context.getString(errorMessageId))
                .setColor(getColor(context, R.color.default_notification_color))
                .setSmallIcon(R.drawable.ic_music_box)
                .setVibrate(new long[]{100L, 100L})
                .setContentIntent(pIntent)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(ERROR_NOTIFICATION_ID, notification);

    }

    public void removeErrorNotification() {
        notificationManager.cancel(ERROR_NOTIFICATION_ID);
    }

    public void startForegroundNotification(Service service,
                                            boolean play,
                                            @Nullable PlayQueueItem queueItem,
                                            MediaSessionCompat mediaSession,
                                            @Nullable MusicNotificationSetting notificationSetting) {
        Notification notification = getDefaultMusicNotification(play,
                queueItem,
                mediaSession,
                notificationSetting)
                .build();
        service.startForeground(FOREGROUND_NOTIFICATION_ID, notification);

        showMusicNotificationWithCover(play, queueItem, mediaSession, notificationSetting);
    }

    public void updateForegroundNotification(boolean play,
                                             @Nullable PlayQueueItem queueItem,
                                             MediaSessionCompat mediaSession,
                                             MusicNotificationSetting notificationSetting) {
        if (!isNotificationVisible(notificationManager, FOREGROUND_NOTIFICATION_ID)) {
            return;
        }

        Notification notification = getDefaultMusicNotification(play,
                queueItem,
                mediaSession,
                notificationSetting)
                .build();
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification);

        showMusicNotificationWithCover(play, queueItem, mediaSession, notificationSetting);
    }

    private void showMusicNotificationWithCover(boolean play,
                                                @Nullable PlayQueueItem queueItem,
                                                MediaSessionCompat mediaSession,
                                                MusicNotificationSetting notificationSetting) {
        if (cancellationRunnable != null) {
            cancellationRunnable.run();
        }

        if (queueItem == null) {
            return;
        }

        boolean showCovers = false;
        if (notificationSetting != null) {
            showCovers = notificationSetting.isShowCovers();
        }
        if (!showCovers) {
            return;
        }

        Composition composition = queueItem.getComposition();

        cancellationRunnable = coverImageLoader.loadNotificationImage(composition, bitmap -> {
            NotificationCompat.Builder builder = getDefaultMusicNotification(play,
                    queueItem,
                    mediaSession,
                    notificationSetting);

            builder.setLargeIcon(bitmap);
            currentNotificationBitmap = bitmap;
            notificationManager.notify(FOREGROUND_NOTIFICATION_ID, builder.build());
        });
    }

    private NotificationCompat.Builder getDefaultMusicNotification(boolean play,
                                                                   @Nullable PlayQueueItem queueItem,
                                                                   MediaSessionCompat mediaSession,
                                                                   @Nullable MusicNotificationSetting notificationSetting) {
        int requestCode = play? PAUSE : PLAY;
        Intent intentPlayPause = new Intent(context, MusicService.class);
        intentPlayPause.putExtra(REQUEST_CODE, requestCode);
        PendingIntent pIntentPlayPause = PendingIntent.getService(context,
                requestCode,
                intentPlayPause,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intentSkipToPrevious = new Intent(context, MusicService.class);
        intentSkipToPrevious.putExtra(REQUEST_CODE, SKIP_TO_PREVIOUS);
        PendingIntent pIntentSkipToPrevious = PendingIntent.getService(context,
                SKIP_TO_PREVIOUS,
                intentSkipToPrevious,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intentSkipToNext = new Intent(context, MusicService.class);
        intentSkipToNext.putExtra(REQUEST_CODE, SKIP_TO_NEXT);
        PendingIntent pIntentSkipToNext = PendingIntent.getService(context,
                SKIP_TO_NEXT,
                intentSkipToNext,
                PendingIntent.FLAG_CANCEL_CURRENT);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(OPEN_PLAY_QUEUE_ARG, true);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                play? R.drawable.ic_pause: R.drawable.ic_play,
                getString(play? R.string.pause: R.string.play),
                pIntentPlayPause);

        androidx.media.app.NotificationCompat.MediaStyle style = new androidx.media.app.NotificationCompat.MediaStyle();
        style.setShowActionsInCompactView(0, 1, 2);
        style.setMediaSession(mediaSession.getSessionToken());

        boolean coloredNotification = false;
        boolean showCovers = false;
        if (notificationSetting != null) {
            coloredNotification = notificationSetting.isColoredNotification();
            showCovers = notificationSetting.isShowCovers();
        }

        NotificationCompat.Builder builder = notificationBuilder.buildMusicNotification(context)
                .setColorized(coloredNotification)
                .setColor(getColor(context, R.color.default_notification_color))
                .setSmallIcon(R.drawable.ic_music_box)
                .setContentIntent(pIntent)
                .addAction(R.drawable.ic_skip_previous, getString(R.string.previous_track), pIntentSkipToPrevious)
                .addAction(playPauseAction)
                .addAction(R.drawable.ic_skip_next, getString(R.string.next_track), pIntentSkipToNext)
                .setShowWhen(false)
                .setStyle(style)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (queueItem != null) {
            Composition composition = queueItem.getComposition();

            if (showCovers) {
                Bitmap bitmap = currentNotificationBitmap;
                if (bitmap == null) {
                    bitmap = coverImageLoader.getDefaultNotificationBitmap();
                }
                builder.setLargeIcon(bitmap);
            }

            builder = builder.setContentTitle(formatCompositionName(composition))
                    .setContentText(formatCompositionAuthor(composition, context));
        }
        return builder;
    }

    private boolean isNotificationVisible(NotificationManager notificationManager,
                                          int notificationId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
            for (StatusBarNotification notification : notifications) {
                if (notification.getId() == notificationId) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getString(@StringRes int resId) {
        return context.getString(resId);
    }
}
