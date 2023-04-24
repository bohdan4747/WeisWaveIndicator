package velox.api.layer1.simpledemo.markers;

import java.util.ArrayList;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import velox.api.layer1.Layer1ApiAdminAdapter;
import velox.api.layer1.Layer1ApiFinishable;
import velox.api.layer1.Layer1ApiInstrumentListener;
import velox.api.layer1.Layer1ApiProvider;
import velox.api.layer1.annotations.Layer1ApiVersion;
import velox.api.layer1.annotations.Layer1ApiVersionValue;
import velox.api.layer1.annotations.Layer1Attachable;
import velox.api.layer1.annotations.Layer1StrategyName;
import velox.api.layer1.common.ListenableHelper;
import velox.api.layer1.common.Log;
import velox.api.layer1.data.BalanceInfo;
import velox.api.layer1.data.ExecutionInfo;
import velox.api.layer1.data.InstrumentInfo;
import velox.api.layer1.data.MarketMode;
import velox.api.layer1.data.OrderInfoUpdate;
import velox.api.layer1.data.StatusInfo;
import velox.api.layer1.data.TradeInfo;
import velox.api.layer1.layers.strategies.interfaces.CalculatedResultListener;
import velox.api.layer1.layers.strategies.interfaces.CustomEventAggregatble;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEvent;
import velox.api.layer1.layers.strategies.interfaces.CustomGeneratedEventAliased;
import velox.api.layer1.layers.strategies.interfaces.InvalidateInterface;
import velox.api.layer1.layers.strategies.interfaces.OnlineCalculatable;
import velox.api.layer1.layers.strategies.interfaces.OnlineValueCalculatorAdapter;
import velox.api.layer1.messages.GeneratedEventInfo;
import velox.api.layer1.messages.Layer1ApiUserMessageAddStrategyUpdateGenerator;
import velox.api.layer1.messages.UserMessageLayersChainCreatedTargeted;
import velox.api.layer1.messages.indicators.DataStructureInterface;
import velox.api.layer1.messages.indicators.DataStructureInterface.TreeResponseInterval;
import velox.api.layer1.messages.indicators.IndicatorColorScheme;
import velox.api.layer1.messages.indicators.IndicatorLineStyle;
import velox.api.layer1.messages.indicators.Layer1ApiDataInterfaceRequestMessage;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator;
import velox.api.layer1.messages.indicators.Layer1ApiUserMessageModifyIndicator.GraphType;
import velox.api.layer1.messages.indicators.StrategyUpdateGenerator;

@Layer1Attachable
@Layer1StrategyName("Weis Wave BohdanChaika")
@Layer1ApiVersion(Layer1ApiVersionValue.VERSION2)
public class Layer1ApiWeisWaveDemo implements
        Layer1ApiFinishable,
        Layer1ApiAdminAdapter,
        Layer1ApiInstrumentListener,
        OnlineCalculatable {
    private static class BarEvent implements CustomGeneratedEvent, DataCoordinateMarker {
        private static final long serialVersionUID = 1L;
        private long time;


        double volume;
        double first;

        double last;

        transient int bodyWidthPx;
        public double getVolume()
        {
            return volume;
        }
        public void setVolume(double volume)
        {
            this.volume = volume;
        }
        public boolean getType()
        {
            return first > last;
        }
        public BarEvent(){
            super();
            this.volume = 0;
            this.first =0;
            this.last =0 ;
        }
        public BarEvent(long time) {
            super();
            this.time = time;
            this.volume = 0;
            this.first = 0;
            this.last = 0;

        }
        public BarEvent(long time,double last) {
            super();
            this.time = time;
            this.first = last;
            this.last = last;
            this.volume = 0;
        }

        public BarEvent(long time,double volume,double first,double last, int bodyWidthPx) {
            super();
            this.time = time;
            this.first = first;
            this.last = last;
            this.volume = volume;
            this.bodyWidthPx = bodyWidthPx;
        }

        public BarEvent(BarEvent other) {
            super();
            this.time = other.time;
            this.volume = other.volume;
            this.first = other.first;
            this.last = other.last;
            this.bodyWidthPx = other.bodyWidthPx;
        }
        public void setTime(long time) {
            this.time = time;
        }

        @Override
        public long getTime() {
            return time;
        }

        @Override
        public Object clone() {
            return new BarEvent(time,volume,first,last,bodyWidthPx);
        }
        @Override
        public String toString() {
            return "[" + time + ": "+ volume + "/" + first +  "/" + last + "]";
        }

        @Override
        public double getMinY() {
            return 0;
        }

        @Override
        public double getMaxY() {
            return volume;
        }

        @Override
        public double getValueY() {
            return 0;
        }

        public void update(double price,long size) {

            volume+=price * size;
            last = price;
            if(first == 0)first = price;
        }

        public void update(BarEvent nextBar) {
            volume += nextBar.volume;
            if(first == 0)first = nextBar.first;
            last = nextBar.last;
        }

        public void setBodyWidthPx(int bodyWidthPx) {
            this.bodyWidthPx = bodyWidthPx;
        }


        @Override
        public Marker makeMarker(Function<Double, Integer> yDataCoordinateToPixelFunction) {

            int height =yDataCoordinateToPixelFunction.apply(volume);
            if(height <= 0){
                return null;
            }

            int width = bodyWidthPx;
            BufferedImage icon = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics g = icon.getGraphics();
            if(getType()){
                int red = 239;
                int green = 84;
                int blue = 84;
                Color color = new Color(red, green, blue);
                g.setColor(color);
            }
            else {
                int red = 51;
                int green = 255;
                int blue = 153;
                Color color = new Color(red, green, blue);
                g.setColor(color);
            }
            g.fillRect(0, 0, width, height);

            int xOffset = -width / 2;

            return new Marker(0, xOffset, 0, icon);

        }

        /**
         * We initially compute everything in level number, like onDepth calls are
         * (prices divided by pips), but if we are going to render it on the bottom
         * panel we want to convert into price
         */
        public void applyPips(double pips) {
            volume*=pips;
            first*=pips;
            last*=pips;
        }
    }

    public static final CustomEventAggregatble BAR_EVENTS_AGGREGATOR = new CustomEventAggregatble() {
        @Override
        public CustomGeneratedEvent getInitialValue(long t) {
            return new BarEvent(t);
        }

        @Override
        public void aggregateAggregationWithValue(CustomGeneratedEvent aggregation, CustomGeneratedEvent value) {
            BarEvent aggregationEvent = (BarEvent) aggregation;
            BarEvent valueEvent = (BarEvent) value;
            aggregationEvent.update(valueEvent);
        }

        @Override
        public void aggregateAggregationWithAggregation(CustomGeneratedEvent aggregation1,
                                                        CustomGeneratedEvent aggregation2) {
            BarEvent aggregationEvent1 = (BarEvent) aggregation1;
            BarEvent aggregationEvent2 = (BarEvent) aggregation2;
            aggregationEvent1.update(aggregationEvent2);
        }
    };

    private static final String INDICATOR_NAME_BARS_BOTTOM = "Bars: bottom panel";
    private static final String INDICATOR_LINE_COLOR_NAME = "Trade markers line";
    private static final Color INDICATOR_LINE_DEFAULT_COLOR = Color.RED;

    private static final String TREE_NAME = "Bars";
    private static final Class<?>[] INTERESTING_CUSTOM_EVENTS = new Class<?>[] { BarEvent.class };

    private static final int MAX_BODY_WIDTH = 50;
    private static final int MIN_BODY_WIDTH = 1;
    private static final long CANDLE_INTERVAL_NS = TimeUnit.SECONDS.toNanos(60);


    private Layer1ApiProvider provider;
    private Map<String, BarEvent> lastBar = new HashMap<>();

    private Map<String, String> indicatorsFullNameToUserName = new HashMap<>();
    private Map<String, String> indicatorsUserNameToFullName = new HashMap<>();

    private Map<String, Double> pipsMap = new ConcurrentHashMap<>();

    private DataStructureInterface dataStructureInterface;

    private BufferedImage tradeIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    private Map<String, BufferedImage> orderIcons = Collections.synchronizedMap(new HashMap<>());

    private Object locker = new Object();

    public Layer1ApiWeisWaveDemo(Layer1ApiProvider provider) {
        this.provider = provider;

        ListenableHelper.addListeners(provider, this);

    }

    @Override
    public void finish() {
        synchronized (indicatorsFullNameToUserName) {
            for (String userName: indicatorsFullNameToUserName.values()) {
                provider.sendUserMessage(new Layer1ApiUserMessageModifyIndicator(Layer1ApiWeisWaveDemo.class, userName, false));
            }
        }

        provider.sendUserMessage(getGeneratorMessage(false));
    }

    private Layer1ApiUserMessageModifyIndicator getUserMessageAdd(String userName, GraphType graphType) {
        return Layer1ApiUserMessageModifyIndicator.builder(Layer1ApiWeisWaveDemo.class, userName)
                .setIsAdd(true)
                .setGraphType(graphType)
                .setOnlineCalculatable(this)
                .setIndicatorColorScheme(new IndicatorColorScheme() {
                    @Override
                    public ColorDescription[] getColors() {
                        return new ColorDescription[] {
                                new ColorDescription(Layer1ApiWeisWaveDemo.class, INDICATOR_LINE_COLOR_NAME, INDICATOR_LINE_DEFAULT_COLOR, false),
                        };
                    }

                    @Override
                    public String getColorFor(Double value) {
                        return INDICATOR_LINE_COLOR_NAME;
                    }

                    @Override
                    public ColorIntervalResponse getColorIntervalsList(double valueFrom, double valueTo) {
                        return new ColorIntervalResponse(new String[] {INDICATOR_LINE_COLOR_NAME}, new double[] {});
                    }
                })
                .setIndicatorLineStyle(IndicatorLineStyle.NONE)
                .build();
    }

    @Override
    public void onUserMessage(Object data) {
        if (data.getClass() == UserMessageLayersChainCreatedTargeted.class) {
            UserMessageLayersChainCreatedTargeted message = (UserMessageLayersChainCreatedTargeted) data;
            if (message.targetClass == getClass()) {
                provider.sendUserMessage(new Layer1ApiDataInterfaceRequestMessage(dataStructureInterface -> this.dataStructureInterface = dataStructureInterface));
                addIndicator(INDICATOR_NAME_BARS_BOTTOM);
                provider.sendUserMessage(getGeneratorMessage(true));
            }
        }
    }

    @Override
    public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
        pipsMap.put(alias, instrumentInfo.pips);
    }

    @Override
    public void onInstrumentRemoved(String alias) {
    }

    @Override
    public void onInstrumentNotFound(String symbol, String exchange, String type) {
    }

    @Override
    public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {
    }

    @Override
    public void calculateValuesInRange(String indicatorName, String indicatorAlias, long t0, long intervalWidth, int intervalsNumber,
                                       CalculatedResultListener listener) {
        String userName = indicatorsFullNameToUserName.get(indicatorName);

        Double pips = pipsMap.get(indicatorAlias);

        List<TreeResponseInterval> result = dataStructureInterface.get(Layer1ApiWeisWaveDemo.class, TREE_NAME, t0,
                intervalWidth, intervalsNumber, indicatorAlias, INTERESTING_CUSTOM_EVENTS);

        int bodyWidth = getBodyWidth(intervalWidth);
        BarEvent lastEvent = null;
        for (int i = 1; i <= intervalsNumber; i++) {

            BarEvent value = getBarEvent(result.get(i));
            if (value != null) {
                value = new BarEvent(value);
                if(lastEvent != null && lastEvent.getType() == value.getType()) {
                    value.setVolume(value.getVolume() + lastEvent.getVolume());
                }
                lastEvent = new BarEvent(value);
                lastBar.put(indicatorAlias,lastEvent);


                value.applyPips(pips);

                value.setBodyWidthPx(bodyWidth);

                listener.provideResponse(value);
            } else {
                listener.provideResponse(Double.NaN);
            }
        }

        listener.setCompleted();
    }

    @Override
    public OnlineValueCalculatorAdapter createOnlineValueCalculator(String indicatorName, String indicatorAlias, long time,
                                                                    Consumer<Object> listener, InvalidateInterface invalidateInterface) {
        String userName = indicatorsFullNameToUserName.get(indicatorName);

        Double pips = pipsMap.get(indicatorAlias);

        return new OnlineValueCalculatorAdapter() {

            int bodyWidth = MAX_BODY_WIDTH;

            @Override
            public void onIntervalWidth(long intervalWidth) {
                this.bodyWidth = getBodyWidth(intervalWidth);
            }

            @Override
            public void onUserMessage(Object data) {
                if (data instanceof CustomGeneratedEventAliased) {
                    CustomGeneratedEventAliased aliasedEvent = (CustomGeneratedEventAliased) data;
                    if (indicatorAlias.equals(aliasedEvent.alias) && aliasedEvent.event instanceof BarEvent) {
                        BarEvent event = (BarEvent)aliasedEvent.event;

                        event = new BarEvent(event);
                        BarEvent lastEvent = lastBar.get(indicatorAlias);
                        if(lastEvent != null && lastEvent.getType() == event.getType()) {
                            event.setVolume(event.getVolume() + lastEvent.getVolume());
                        }
                        lastBar.put(indicatorAlias, new BarEvent(event));
                        event.setBodyWidthPx(bodyWidth);
                        event.applyPips(pips);

                        listener.accept(event);
                    }
                }
            }
        };
    }

    private int getBodyWidth(long intervalWidth) {

        long bodyWidth = CANDLE_INTERVAL_NS / intervalWidth;
        bodyWidth = Math.max(bodyWidth, MIN_BODY_WIDTH);
        bodyWidth = Math.min(bodyWidth, MAX_BODY_WIDTH);
        return (int) bodyWidth;

    }

    private BarEvent getBarEvent(TreeResponseInterval treeResponseInterval) {
        Object result = treeResponseInterval.events.get(BarEvent.class.toString());
        if (result != null) {
            return (BarEvent) result;
        } else {
            return null;
        }
    }

    public void addIndicator(String userName) {
        Layer1ApiUserMessageModifyIndicator message = null;
        switch (userName) {
            case INDICATOR_NAME_BARS_BOTTOM:
                message = getUserMessageAdd(userName, GraphType.BOTTOM);
                break;
            default:
                Log.warn("Unknwon name for marker indicator: " + userName);
                break;
        }

        if (message != null) {
            synchronized (indicatorsFullNameToUserName) {
                indicatorsFullNameToUserName.put(message.fullName, message.userName);
                indicatorsUserNameToFullName.put(message.userName, message.fullName);
            }
            provider.sendUserMessage(message);
        }
    }

    private Layer1ApiUserMessageAddStrategyUpdateGenerator getGeneratorMessage(boolean isAdd) {
        return new Layer1ApiUserMessageAddStrategyUpdateGenerator(Layer1ApiWeisWaveDemo.class, TREE_NAME, isAdd, true, new StrategyUpdateGenerator() {
            private Consumer<CustomGeneratedEventAliased> consumer;

            private long time = 0;

            private Map<String, BarEvent> aliasToLastBar = new HashMap<>();

            @Override
            public void setGeneratedEventsConsumer(Consumer<CustomGeneratedEventAliased> consumer) {
                this.consumer = consumer;
            }

            @Override
            public Consumer<CustomGeneratedEventAliased> getGeneratedEventsConsumer() {
                return consumer;
            }

            @Override
            public void onStatus(StatusInfo statusInfo) {
            }

            @Override
            public void onOrderUpdated(OrderInfoUpdate orderInfoUpdate) {
            }

            @Override
            public void onOrderExecuted(ExecutionInfo executionInfo) {
            }

            @Override
            public void onBalance(BalanceInfo balanceInfo) {
            }

            @Override
            public void onTrade(String alias, double price, int size, TradeInfo tradeInfo) {
                BarEvent bar = aliasToLastBar.get(alias);

                long barStartTime = getBarStartTime(time);

                if (bar == null) {
                    bar = new BarEvent(barStartTime);
                    aliasToLastBar.put(alias, bar);
                }

                if (barStartTime != bar.time) {
                    bar.setTime(time);
                    consumer.accept(new CustomGeneratedEventAliased(bar, alias));
                    bar = new BarEvent(barStartTime,bar.last);
                    aliasToLastBar.put(alias, bar);
                }

                if (size != 0) {
                    bar.update(price,size);
                }
            }

            @Override
            public void onMarketMode(String alias, MarketMode marketMode) {
            }

            @Override
            public void onDepth(String alias, boolean isBid, int price, int size) {
            }

            @Override
            public void onInstrumentAdded(String alias, InstrumentInfo instrumentInfo) {
            }

            @Override
            public void onInstrumentRemoved(String alias) {
                aliasToLastBar.remove(alias);
            }

            @Override
            public void onInstrumentNotFound(String symbol, String exchange, String type) {
            }

            @Override
            public void onInstrumentAlreadySubscribed(String symbol, String exchange, String type) {
            }

            @Override
            public void onUserMessage(Object data) {
            }

            @Override
            public void setTime(long time) {
                this.time = time;
                long barStartTime = getBarStartTime(time);
                for (Entry<String, BarEvent> entry : aliasToLastBar.entrySet()) {
                    String alias = entry.getKey();
                    BarEvent bar = entry.getValue();

                    if (barStartTime != bar.time) {
                        bar.setTime(time);
                        consumer.accept(new CustomGeneratedEventAliased(bar, alias));
                        bar = new BarEvent(barStartTime,bar.last);
                        entry.setValue(bar);
                    }
                }
            }
        }, new GeneratedEventInfo[] {new GeneratedEventInfo(BarEvent.class, BarEvent.class, BAR_EVENTS_AGGREGATOR)});
    }

    private long getBarStartTime(long time) {
        return time - time % CANDLE_INTERVAL_NS;
    }
}

