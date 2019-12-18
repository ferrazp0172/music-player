package com.github.anrimian.musicplayer.ui.library.genres.list;

import com.github.anrimian.musicplayer.domain.business.library.LibraryGenresInteractor;
import com.github.anrimian.musicplayer.domain.models.composition.order.Order;
import com.github.anrimian.musicplayer.domain.models.genres.Genre;
import com.github.anrimian.musicplayer.domain.utils.TextUtils;
import com.github.anrimian.musicplayer.ui.common.error.ErrorCommand;
import com.github.anrimian.musicplayer.ui.common.error.parser.ErrorParser;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import moxy.InjectViewState;
import moxy.MvpPresenter;

import static com.github.anrimian.musicplayer.data.utils.rx.RxUtils.dispose;

@InjectViewState
public class GenresListPresenter extends MvpPresenter<GenresListView> {

    private final LibraryGenresInteractor interactor;
    private final ErrorParser errorParser;
    private final Scheduler uiScheduler;

    private final CompositeDisposable presenterDisposable = new CompositeDisposable();
    private Disposable listDisposable;

    private List<Genre> genres = new ArrayList<>();

    @Nullable
    private String searchText;

    public GenresListPresenter(LibraryGenresInteractor interactor,
                               ErrorParser errorParser,
                               Scheduler uiScheduler) {
        this.interactor = interactor;
        this.errorParser = errorParser;
        this.uiScheduler = uiScheduler;
    }

    @Override
    protected void onFirstViewAttach() {
        super.onFirstViewAttach();
        subscribeOnGenresList();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenterDisposable.dispose();
    }


    void onTryAgainLoadCompositionsClicked() {
        subscribeOnGenresList();
    }

    void onOrderMenuItemClicked() {
        getViewState().showSelectOrderScreen(interactor.getOrder());
    }

    void onOrderSelected(Order order) {
        interactor.setOrder(order);
    }

    void onSearchTextChanged(String text) {
        if (!TextUtils.equals(searchText, text)) {
            searchText = text;
            subscribeOnGenresList();
        }
    }

    @Nullable
    String getSearchText() {
        return searchText;
    }

    private void subscribeOnGenresList() {
        if (genres.isEmpty()) {
            getViewState().showLoading();
        }
        dispose(listDisposable, presenterDisposable);
        listDisposable = interactor.getGenresObservable(searchText)
                .observeOn(uiScheduler)
                .subscribe(this::onGenresReceived, this::onGenresReceivingError);
        presenterDisposable.add(listDisposable);
    }

    private void onGenresReceivingError(Throwable throwable) {
        ErrorCommand errorCommand = errorParser.parseError(throwable);
        getViewState().showLoadingError(errorCommand);
    }

    private void onGenresReceived(List<Genre> genres) {
        this.genres = genres;
        getViewState().submitList(genres);
        if (genres.isEmpty()) {
            if (TextUtils.isEmpty(searchText)) {
                getViewState().showEmptyList();
            } else {
                getViewState().showEmptySearchResult();
            }
        } else {
            getViewState().showList();
        }
    }
}
