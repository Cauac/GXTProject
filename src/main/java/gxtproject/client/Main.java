package gxtproject.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.ImageCell;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.editor.client.Editor.Path;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBean.PropertyName;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.sencha.gxt.core.client.ValueProvider;
import com.sencha.gxt.data.client.loader.HttpProxy;
import com.sencha.gxt.data.client.loader.ScriptTagProxy;
import com.sencha.gxt.data.client.loader.StorageReadProxy;
import com.sencha.gxt.data.client.loader.StorageWriteProxy;
import com.sencha.gxt.data.client.loader.StorageWriteProxy.Entry;
import com.sencha.gxt.data.client.writer.UrlEncodingWriter;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.ModelKeyProvider;
import com.sencha.gxt.data.shared.PropertyAccess;
import com.sencha.gxt.data.shared.loader.DataProxy;
import com.sencha.gxt.data.shared.loader.JsonReader;
import com.sencha.gxt.data.shared.loader.LoadResultListStoreBinding;
import com.sencha.gxt.data.shared.loader.PagingLoadConfig;
import com.sencha.gxt.data.shared.loader.PagingLoadResult;
import com.sencha.gxt.data.shared.loader.PagingLoader;
import com.sencha.gxt.widget.core.client.FramedPanel;
import com.sencha.gxt.widget.core.client.box.MessageBox;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.BoxLayoutContainer.BoxLayoutPack;
import com.sencha.gxt.widget.core.client.container.SimpleContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer;
import com.sencha.gxt.widget.core.client.container.VerticalLayoutContainer.VerticalLayoutData;
import com.sencha.gxt.widget.core.client.container.Viewport;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent.SelectHandler;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.toolbar.PagingToolBar;

public class Main implements IsWidget, EntryPoint {
    private FramedPanel fp;

    interface TestAutoBeanFactory extends AutoBeanFactory {
        static TestAutoBeanFactory instance = GWT.create(TestAutoBeanFactory.class);

        AutoBean<PhotoCollection> dataCollection();

        AutoBean<PhotoListLoadResult> dataLoadResult();

        AutoBean<PhotoLoadConfig> loadConfig();
    }

    public interface Photo {

        public String getTitle();

        public String getOwnername();

        public String getViews();

        public String getUrl_sq();

        public Date getDateupload();

        public Date getLastupdate();


    }

    interface PhotoCollection {
        Integer getTotalCount();

        List<Photo> getTopics();
    }

    interface PhotoLoadConfig extends PagingLoadConfig {

        Integer getQuery();

        void setQuery(String query);

        @Override
        @PropertyName("start")
        public int getOffset();

        @Override
        @PropertyName("start")
        public void setOffset(int offset);
    }

    interface PhotoListLoadResult extends PagingLoadResult<Photo> {
        void setData(List<Photo> data);

        @Override
        @PropertyName("start")
        public int getOffset();

        @Override
        @PropertyName("start")
        public void setOffset(int offset);
    }

    interface PhotoProperties extends PropertyAccess<Photo> {
        @Path("title")
        ModelKeyProvider<Photo> key();

        ValueProvider<Photo, String> title();

        ValueProvider<Photo, String> ownername();

        ValueProvider<Photo, String> views();

        ValueProvider<Photo, String> url_sq();

        ValueProvider<Photo, Date> dateupload();

        ValueProvider<Photo, Date> lastupdate();


    }

    @Override
    public void onModuleLoad() {
        Viewport viewport = new Viewport();
        viewport.add(this);
        RootPanel.get().add(viewport);
    }

    @Override
    public Widget asWidget() {
        if (fp == null) {

            final Storage storage = Storage.getLocalStorageIfSupported();
            if (storage == null) {
                new MessageBox("Not Supported", "Your browser doesn't appear to supprt HTML5 localStorage").show();
                return new HTML("LocalStorage not supported in this browser");
            }

            // Writer to translate load config into string
            UrlEncodingWriter<PhotoLoadConfig> writer = new UrlEncodingWriter<PhotoLoadConfig>(TestAutoBeanFactory.instance, PhotoLoadConfig.class);
            // Reader to translate String results into objects
            JsonReader<PhotoListLoadResult, PhotoCollection> reader = new JsonReader<PhotoListLoadResult, PhotoCollection>(
                    TestAutoBeanFactory.instance, PhotoCollection.class) {
                @Override
                protected PhotoListLoadResult createReturnData(Object loadConfig, PhotoCollection records) {
                    PagingLoadConfig cfg = (PagingLoadConfig) loadConfig;
                    PhotoListLoadResult res = TestAutoBeanFactory.instance.dataLoadResult().as();
                    res.setData(records.getTopics());
                    res.setOffset(cfg.getOffset());
                    res.setTotalLength(records.getTotalCount());
                    return res;
                }
            };

            // Proxy to load from server
            String url = "/photo/getPhotosList.json";
            RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
            final HttpProxy<PhotoLoadConfig> remoteProxy = new HttpProxy<PhotoLoadConfig>(builder);
            remoteProxy.setWriter(writer);

            // Proxy to load objects from local storage
            final StorageReadProxy<PhotoLoadConfig> localReadProxy = new StorageReadProxy<PhotoLoadConfig>(storage);
            localReadProxy.setWriter(writer);

            // Proxy to persist network-loaded objects into local storage
            final StorageWriteProxy<PhotoLoadConfig, String> localWriteProxy = new StorageWriteProxy<PhotoLoadConfig, String>(storage);
            localWriteProxy.setKeyWriter(writer);

            // Wrapper Proxy to dispatch to either storage or scripttag, and to save results
            DataProxy<PhotoLoadConfig, String> proxy = new DataProxy<PhotoLoadConfig, String>() {
                @Override
                public void load(final PhotoLoadConfig loadConfig, final Callback<String, Throwable> callback) {
                    // Storage read is known to be synchronous, so read it first - if null, continue
                    localReadProxy.load(loadConfig, new Callback<String, Throwable>() {
                        @Override
                        public void onFailure(Throwable reason) {
                            // ignore failure, go remote
                            onSuccess(null);
                        }

                        @Override
                        public void onSuccess(String result) {
                            if (result != null) {
                                callback.onSuccess(result);
                            } else {
                                //read from remote and save it
                                remoteProxy.load(loadConfig, new Callback<String, Throwable>() {
                                    @Override
                                    public void onSuccess(String result) {
                                        Entry<PhotoLoadConfig, String> data = new Entry<PhotoLoadConfig, String>(loadConfig, result);
                                        localWriteProxy.load(data, new Callback<Void, Throwable>() {
                                            @Override
                                            public void onSuccess(Void result) {
                                                // ignore response
                                            }

                                            @Override
                                            public void onFailure(Throwable reason) {
                                                // ignore response
                                            }
                                        });
                                        callback.onSuccess(result);
                                    }

                                    @Override
                                    public void onFailure(Throwable reason) {
                                        callback.onFailure(reason);
                                    }
                                });
                            }
                        }
                    });
                }
            };


            PagingLoader<PhotoLoadConfig, PhotoListLoadResult> loader = new PagingLoader<PhotoLoadConfig, PhotoListLoadResult>(
                    proxy, reader);
            loader.useLoadConfig(TestAutoBeanFactory.instance.loadConfig().as());

            PhotoProperties props = GWT.create(PhotoProperties.class);

            ListStore<Photo> store = new ListStore<Photo>(props.key());
            loader.addLoadHandler(new LoadResultListStoreBinding<PhotoLoadConfig, Photo, PhotoListLoadResult>(store));

            ColumnConfig<Photo, String> cc1 = new ColumnConfig<Photo, String>(props.title(), 100, "Title");
            ColumnConfig<Photo, String> cc2 = new ColumnConfig<Photo, String>(props.ownername(), 100, "Owner Name");
            ColumnConfig<Photo, String> cc3 = new ColumnConfig<Photo, String>(props.views(), 100, "Views");
            ColumnConfig<Photo, Date> cc4 = new ColumnConfig<Photo, Date>(props.dateupload(), 100, "Date Upload");
            cc4.setCell(new DateCell());
            ColumnConfig<Photo, Date> cc5 = new ColumnConfig<Photo, Date>(props.dateupload(), 100, "Last Update");
            cc5.setCell(new DateCell());
            ColumnConfig<Photo, String> cc6 = new ColumnConfig<Photo, String>(props.url_sq(), 100, "Photo");
            cc6.setCell(new ImageCell());

            List<ColumnConfig<Photo, ?>> l = new ArrayList<ColumnConfig<Photo, ?>>();
            l.add(cc1);
            l.add(cc2);
            l.add(cc3);
            l.add(cc4);
            l.add(cc5);
            l.add(cc6);
            ColumnModel<Photo> cm = new ColumnModel<Photo>(l);

            Grid<Photo> grid = new Grid<Photo>(store, cm);
            grid.getView().setForceFit(true);
            grid.setLoader(loader);
            grid.setLoadMask(true);
            grid.setBorders(true);

            final PagingToolBar toolBar = new PagingToolBar(10);
            toolBar.getElement().getStyle().setProperty("borderBottom", "none");
            toolBar.add(new TextButton("Clear Cache", new SelectHandler() {
                @Override
                public void onSelect(SelectEvent event) {
                    storage.clear();
                }
            }));
            toolBar.bind(loader);

            fp = new FramedPanel();
            fp.setHeadingText("LocalStorage Grid Example");
            fp.setCollapsible(true);
            fp.setAnimCollapse(true);
            fp.setWidth("100%");
            fp.setHeight(500);

            fp.addStyleName("margin-10");
            fp.setButtonAlign(BoxLayoutPack.CENTER);

            VerticalLayoutContainer con = new VerticalLayoutContainer();
            con.setBorders(true);
            con.add(grid, new VerticalLayoutData(1, 1));
            con.add(toolBar, new VerticalLayoutData(1, -1));
            fp.setWidget(con);

            loader.load();
        }
        return fp;
    }

}