package com.github.anrimian.musicplayer.data.repositories.playlists;

import com.github.anrimian.musicplayer.data.models.StoragePlayList;
import com.github.anrimian.musicplayer.data.models.exceptions.PlayListNotFoundException;
import com.github.anrimian.musicplayer.data.repositories.playlists.comparators.PlayListModifyDateComparator;
import com.github.anrimian.musicplayer.data.storage.providers.music.StorageMusicDataSource;
import com.github.anrimian.musicplayer.data.storage.providers.playlists.PlayListDataSource;
import com.github.anrimian.musicplayer.data.storage.providers.playlists.StoragePlayListItem;
import com.github.anrimian.musicplayer.data.storage.providers.playlists.StoragePlayListsProvider;
import com.github.anrimian.musicplayer.domain.models.composition.Composition;
import com.github.anrimian.musicplayer.domain.models.playlist.PlayList;
import com.github.anrimian.musicplayer.domain.models.playlist.PlayListItem;
import com.github.anrimian.musicplayer.domain.repositories.PlayListsRepository;
import com.github.anrimian.musicplayer.domain.utils.Objects;
import com.github.anrimian.musicplayer.domain.utils.changes.MapChangeProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;

import static com.github.anrimian.musicplayer.data.utils.rx.RxUtils.withDefaultValue;
import static com.github.anrimian.musicplayer.domain.models.utils.PlayListItemHelper.getTotalDuration;

public class PlayListsRepositoryImpl implements PlayListsRepository {

    private final StoragePlayListsProvider storagePlayListsProvider;
    private final StorageMusicDataSource storageMusicDataSource;
    private final Scheduler scheduler;

    private final BehaviorSubject<Map<Long, PlayListDataSource>> playListsSubject = BehaviorSubject.create();

    private Disposable changeDisposable;
    private Map<Long, StoragePlayList> playListMap;

    public PlayListsRepositoryImpl(StoragePlayListsProvider storagePlayListsProvider,
                                   StorageMusicDataSource storageMusicDataSource,
                                   Scheduler scheduler) {
        this.storagePlayListsProvider = storagePlayListsProvider;
        this.storageMusicDataSource = storageMusicDataSource;
        this.scheduler = scheduler;
    }

    @Override
    public Observable<List<PlayList>> getPlayListsObservable() {
        return withDefaultValue(playListsSubject, this::getPlayListsMap)
                .flatMapSingle(this::toPlayLists)
                .map(this::toSortedPlayLists)
//                .debounce(new FastDebounceFilter<>())//issues with often updates
                .subscribeOn(scheduler);
    }

    @Override
    public Observable<PlayList> getPlayListObservable(long playlistId) {
        PlayListDataSource playListFullModel = getPlayListsMap().get(playlistId);
        Observable<PlayList> observable;
        if (playListFullModel == null) {
            observable = Observable.error(new PlayListNotFoundException());
        } else {
            observable = playListFullModel.getPlayListObservable();
        }
        return observable.subscribeOn(scheduler);
    }

    @Override
    public Observable<List<PlayListItem>> getCompositionsObservable(long playlistId) {
        PlayListDataSource playListFullModel = getPlayListsMap().get(playlistId);
        Observable<List<PlayListItem>> observable;
        if (playListFullModel == null) {
            observable = Observable.error(new PlayListNotFoundException());
        } else {
            observable = playListFullModel.getPlayListItemsObservable();
        }
        return observable.subscribeOn(scheduler);
    }

    @Override
    public Single<PlayList> createPlayList(String name) {
        return Single.fromCallable(() -> storagePlayListsProvider.createPlayList(name))
                .map(playList -> toPlayList(playList, Collections.emptyList()))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable addCompositionsToPlayList(List<Composition> compositions,
                                                 PlayList playList,
                                                 int position) {
        return Completable.fromAction(() -> storagePlayListsProvider.addCompositionsToPlayList(
                compositions,
                playList.getId(),
                position)
        ).subscribeOn(scheduler);
    }

    @Override
    public Completable addCompositionsToPlayList(List<Composition> compositions, PlayList playList) {
        return Completable.fromAction(() -> storagePlayListsProvider.addCompositionsToPlayList(
                compositions,
                playList.getId(),
                playList.getCompositionsCount())
        ).subscribeOn(scheduler);
    }

    @Override
    public Completable deleteItemFromPlayList(long itemId, long playListId) {
        return Completable.fromAction(() ->
                storagePlayListsProvider.deleteItemFromPlayList(itemId, playListId)
        ).subscribeOn(scheduler);
    }

    @Override
    public Completable deletePlayList(long playListId) {
        return Completable.fromAction(() -> storagePlayListsProvider.deletePlayList(playListId))
                .subscribeOn(scheduler);
    }

    @Override
    public Completable moveItemInPlayList(long playListId, int from, int to) {
        return Completable.fromAction(() -> storagePlayListsProvider.moveItemInPlayList(
                playListId,
                from,
                to)
        ).subscribeOn(scheduler);
    }

    @Override
    public Completable updatePlayListName(long playListId, String name) {
        return Completable.fromAction(() -> storagePlayListsProvider.updatePlayListName(
                playListId, name)
        ).subscribeOn(scheduler);
    }

    //TODO catch concurrent modification exception
    private List<PlayList> toSortedPlayLists(List<PlayList> list) {
        Collections.sort(list, new PlayListModifyDateComparator());
        return list;
    }

    private Single<List<PlayList>> toPlayLists(Map<Long, PlayListDataSource> map) {
        return Observable.fromIterable(map.values())
                .flatMapSingle(PlayListDataSource::getPlayList)
                .collect(ArrayList::new, List::add);
    }

    private Map<Long, PlayListDataSource> getPlayListsMap() {
        Map<Long, PlayListDataSource> playListMap = playListsSubject.getValue();
        if (playListMap == null) {
            synchronized (this) {
                this.playListMap = storagePlayListsProvider.getPlayLists();
                playListMap = createPlayListMap(this.playListMap);
                subscribeOnPlayListChanges();

            }
        }
        return playListMap;
    }

    private void subscribeOnPlayListChanges() {
        changeDisposable = storagePlayListsProvider.getChangeObservable()
                .subscribeOn(scheduler)
                .subscribe(this::onPlayListsChanged);
    }

    private void onPlayListsChanged(Map<Long, StoragePlayList> newPlayListMap) {
        Map<Long, PlayListDataSource> playListDataSourceMap = playListsSubject.getValue();
        boolean updated = MapChangeProcessor.processChanges(playListMap,
                newPlayListMap,
                this::hasDirectChanges,
                deletedEntry -> playListDataSourceMap.remove(deletedEntry.getKey()).dispose(),
                this::onNewPlayListReceived,
                modifiedEntry -> playListDataSourceMap.get(modifiedEntry.getKey()).updatePlayList(modifiedEntry.getValue()));

        if (updated) {
            playListsSubject.onNext(playListDataSourceMap);
        }
    }

    private void onNewPlayListReceived(Map.Entry<Long, StoragePlayList> entry) {
        Map<Long, PlayListDataSource> playListDataSourceMap = playListsSubject.getValue();

        StoragePlayList storagePlayList = entry.getValue();
        PlayListDataSource playListDataSource = new PlayListDataSource(storagePlayList,
                storagePlayListsProvider,
                storageMusicDataSource);
        playListDataSourceMap.put(entry.getKey(), playListDataSource);
    }

    private boolean hasDirectChanges(@Nonnull StoragePlayList first,
                                     @Nonnull StoragePlayList second) {
        return !Objects.equals(first.getName(), second.getName())
                || !Objects.equals(first.getDateAdded(), second.getDateAdded())
                || !Objects.equals(first.getDateModified(), second.getDateModified());
    }

    private Map<Long, PlayListDataSource> createPlayListMap(Map<Long, StoragePlayList> storagePlayLists) {
        Map<Long, PlayListDataSource> playListMap = new ConcurrentHashMap<>();
        for (StoragePlayList storagePlayList: storagePlayLists.values()) {
            long id = storagePlayList.getId();
//            List<StoragePlayListItem> storageItems = storagePlayListsProvider.getPlayListItems(id);
//            List<PlayListItem> items = createPlayList(storageItems, storageMusicDataSource.getCompositionsMap());
//            PlayList playList = toPlayList(storagePlayList, items);

            PlayListDataSource playListDataSource = new PlayListDataSource(storagePlayList,
                    storagePlayListsProvider,
                    storageMusicDataSource);
            playListMap.put(id, playListDataSource);
        }
        return playListMap;
    }

    private PlayList toPlayList(StoragePlayList storagePlayList, List<PlayListItem> items) {
        return new PlayList(storagePlayList.getId(),
                storagePlayList.getName(),
                storagePlayList.getDateAdded(),
                storagePlayList.getDateModified(),
                items.size(),
                getTotalDuration(items));
    }

    private List<PlayListItem> createPlayList(List<StoragePlayListItem> items, Map<Long, Composition> compositionMap) {
        List<PlayListItem> playListItems = new ArrayList<>(items.size());
        for (StoragePlayListItem item: items) {
            Composition composition = compositionMap.get(item.getCompositionId());
            if (composition == null) {
                continue;
            }
            playListItems.add(new PlayListItem(item.getItemId(), composition));
        }
        return playListItems;
    }
}
