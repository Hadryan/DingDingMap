package com.dingmouren.dingdingmap.ui.route_plan;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.TabLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RidePath;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.dingmouren.dingdingmap.MyApplication;
import com.dingmouren.dingdingmap.R;
import com.dingmouren.dingdingmap.base.BaseActivity;
import com.dingmouren.dingdingmap.ui.adapter.RoutePlanBusAdapter;
import com.dingmouren.dingdingmap.ui.detail.BusRouteDetailActivity;
import com.dingmouren.dingdingmap.ui.detail.DriveRouteDetailActivity;
import com.dingmouren.dingdingmap.ui.detail.RideRouteDetailActivity;
import com.dingmouren.dingdingmap.ui.detail.WalkRouteDetailActivity;
import com.dingmouren.dingdingmap.util.AMapUtil;
import com.dingmouren.dingdingmap.util.DrivingRouteOverlay;
import com.dingmouren.dingdingmap.util.RideRouteOverlay;
import com.dingmouren.dingdingmap.util.WalkRouteOverlay;
import com.orhanobut.logger.Logger;
import com.rengwuxian.materialedittext.MaterialEditText;

import butterknife.BindView;

/**
 * Created by dingmouren on 2017/3/1.
 */

public class RoutePlanActivity extends BaseActivity implements AMap.OnMapClickListener,AMap.OnMarkerClickListener
                        ,AMap.OnInfoWindowClickListener,AMap.InfoWindowAdapter,RouteSearch.OnRouteSearchListener{
    private static final String TAG = RoutePlanActivity.class.getName();
    @BindView(R.id.edit_start)  MaterialEditText mEditStart;
    @BindView(R.id.edit_end) MaterialEditText mEditEnd;
    @BindView(R.id.img_back) ImageView mImgBack;
    @BindView(R.id.img_return) ImageView mImgReturn;
    @BindView(R.id.tab_layout) TabLayout mTabLayout;
    @BindView(R.id.route_map)  MapView mMapView;
    @BindView(R.id.bottom_info)  RelativeLayout mBottomInfo;
    @BindView(R.id.recycler)  RecyclerView mRecycler;
    @BindView(R.id.firstline)   TextView mFirstLine;
    @BindView(R.id.secondline) TextView mSeconLine;
    @BindView(R.id.detail)   LinearLayout mDetail;
    @BindView(R.id.progressbar) ProgressBar mProgressBar;
    @BindView(R.id.tv_logo) TextView mTvLogo;

    private AMap mAMap;
    private RouteSearch mRouteSearch;
    private DriveRouteResult mDriveRouteResult;
    private BusRouteResult mBusRouteResult;
    private WalkRouteResult mWalkRouteResult;
    private RideRouteResult mRideRouteResult;
    private LatLonPoint mStartPoint ;//起点，
    private LatLonPoint mEndPoint ;//终点，
    private String mCurrentCityName ;
    private final int ROUTE_TYPE_DRIVE = 0;
    private final int ROUTE_TYPE_BUS = 1;
    private final int ROUTE_TYPE_WALK = 2;
    private final int ROUTE_TYPE_RIDE = 3;
    private RoutePlanBusAdapter mBusAdapter;
    private String mOrigin;
    private String mTarget;
    private UiSettings mUiSetting;

    public static String[] ways = new String[]{"驾车","公交","步行","骑行"};

    public static void newInstance(Activity activity,LatLonPoint startPoint,LatLonPoint endPoint,String cityName,String target){
        Intent intent = new Intent(activity,RoutePlanActivity.class);
        intent.putExtra("start_point",startPoint);
        intent.putExtra("end_point",endPoint);
        intent.putExtra("city_name",cityName);
        intent.putExtra("target",target);
        activity.startActivity(intent);
    }
    @Override
    public int setLayoutId() {
        return R.layout.activity_route_plan;
    }

    @Override
    public void init(Bundle savedInstanceStae) {
        if (null != getIntent()){
            mStartPoint = getIntent().getParcelableExtra("start_point");
            mEndPoint = getIntent().getParcelableExtra("end_point");
            mCurrentCityName = getIntent().getStringExtra("city_name");
            mTarget = getIntent().getStringExtra("target");
            Log.e(TAG,"mCurrentCityName:" + mCurrentCityName);
        }

    }

    @Override
    public void initView(Bundle savedInstanceState) {
        mMapView.onCreate(savedInstanceState);
        if (null == mAMap) mAMap = mMapView.getMap();
        if (null == mUiSetting && null != mAMap){
            mUiSetting = mAMap.getUiSettings();
            mUiSetting.setLogoLeftMargin(getWindowManager().getDefaultDisplay().getWidth());//隐藏高德地图的Logo
        }
        mRouteSearch = new RouteSearch(this);
        if (null != mStartPoint && null != mEndPoint) {
            mAMap.addMarker(new MarkerOptions().position(AMapUtil.convertToLatLng(mStartPoint)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.start)));
            mAMap.addMarker(new MarkerOptions().position(AMapUtil.convertToLatLng(mEndPoint)).icon(BitmapDescriptorFactory.fromResource(R.mipmap.end)));
        }

        for (int i = 0; i < ways.length; i++) {
            mTabLayout.addTab(mTabLayout.newTab().setText(ways[i]));
        }
        mTabLayout.setScrollPosition(1,0,true);//滑动到指定为tab
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setHasFixedSize(true);

        if (mTarget != null){
            mEditEnd.setText(mTarget,TextView.BufferType.NORMAL);
        }
    }

    @Override
    public void initListener() {
        if (null != mRouteSearch) mRouteSearch.setRouteSearchListener(this);
        mAMap.setOnMapClickListener(RoutePlanActivity.this);
        mAMap.setOnMarkerClickListener(RoutePlanActivity.this);
        mAMap.setOnInfoWindowClickListener(RoutePlanActivity.this);
        mAMap.setInfoWindowAdapter(RoutePlanActivity.this);

        mImgBack.setOnClickListener(v -> finish());
        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()){
                    case ROUTE_TYPE_DRIVE:
                        mMapView.setVisibility(View.VISIBLE);
                        mRecycler.setVisibility(View.GONE);
                        mBottomInfo.setVisibility(View.VISIBLE);
                        mTvLogo.setVisibility(View.VISIBLE);
                        searchRouteResult(ROUTE_TYPE_DRIVE,RouteSearch.DRIVING_SINGLE_DEFAULT);
                        break;
                    case ROUTE_TYPE_BUS:
                        mTvLogo.setVisibility(View.GONE);
                        searchBusRoute();
                        break;
                    case ROUTE_TYPE_WALK:
                        mMapView.setVisibility(View.VISIBLE);
                        mRecycler.setVisibility(View.GONE);
                        mBottomInfo.setVisibility(View.VISIBLE);
                        mTvLogo.setVisibility(View.VISIBLE);
                        searchRouteResult(ROUTE_TYPE_WALK,RouteSearch.WALK_DEFAULT);
                        break;
                    case ROUTE_TYPE_RIDE:
                        mMapView.setVisibility(View.VISIBLE);
                        mRecycler.setVisibility(View.GONE);
                        mBottomInfo.setVisibility(View.VISIBLE);
                        mTvLogo.setVisibility(View.VISIBLE);
                        searchRouteResult(ROUTE_TYPE_RIDE,RouteSearch.RIDING_DEFAULT);
                        break;
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });


    }

    @Override
    public void initData() {
        searchBusRoute();//初始化公交车路线数据
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    /**
     * 公交路线搜索
     */
    private void searchBusRoute(){
        mMapView.setVisibility(View.GONE);
        mRecycler.setVisibility(View.VISIBLE);
        mBottomInfo.setVisibility(View.GONE);
        searchRouteResult(ROUTE_TYPE_BUS,RouteSearch.BUS_LEASE_WALK);
    }

    /**
     * 开始搜索路线
     * @param routeType
     * @param mode
     */
    private void searchRouteResult(int routeType, int mode){
        if (null == mStartPoint){
            Toast.makeText(MyApplication.applicationContext,"起点未设置",Toast.LENGTH_SHORT).show();
            return;
        }
        if (null == mEndPoint){
            Toast.makeText(MyApplication.applicationContext,"终点未设置",Toast.LENGTH_SHORT).show();
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        final RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(mStartPoint,mEndPoint);
        switch (routeType){
            case ROUTE_TYPE_DRIVE:
                //第三个参数表示途经点，第四个参数表示避让区域，第五个参数表示避让道路
                RouteSearch.DriveRouteQuery driveRouteQuery = new RouteSearch.DriveRouteQuery(fromAndTo,mode,null,null,"");
                mRouteSearch.calculateDriveRouteAsyn(driveRouteQuery);
                break;
            case ROUTE_TYPE_BUS:
                Log.e(TAG,"mCurrentCityName:" + mCurrentCityName);
                RouteSearch.BusRouteQuery busRouteQuery = new RouteSearch.BusRouteQuery(fromAndTo,mode,mCurrentCityName,0);//0表示不计算夜班车
                mRouteSearch.calculateBusRouteAsyn(busRouteQuery);
                break;
            case ROUTE_TYPE_WALK:
                RouteSearch.WalkRouteQuery walkRouteQuery = new RouteSearch.WalkRouteQuery(fromAndTo,mode);
                mRouteSearch.calculateWalkRouteAsyn(walkRouteQuery);
                break;
            case ROUTE_TYPE_RIDE:
                RouteSearch.RideRouteQuery rideRouteQuery = new RouteSearch.RideRouteQuery(fromAndTo,mode);
                mRouteSearch.calculateRideRouteAsyn(rideRouteQuery);
                break;
        }
    }


    @Override//OnMapClickListener
    public void onMapClick(LatLng latLng) {

    }

    @Override//OnMarkerClickListener
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override//OnInfoWindowClickListener
    public void onInfoWindowClick(Marker marker) {

    }

    @Override//InfoWindowAdapter-1
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override//InfoWindowAdapter-2
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override//公交路线搜索结果方法回调
    public void onBusRouteSearched(BusRouteResult result, int rCode) {
        mProgressBar.setVisibility(View.GONE);
        mBottomInfo.setVisibility(View.GONE);
        mAMap.clear();//清空地图上的覆盖物
        if (rCode == AMapException.CODE_AMAP_SUCCESS){
            if (result != null && result.getPaths() != null) {
                if (result.getPaths().size() > 0) {
                    mBusRouteResult = result;
                   mBusAdapter = new RoutePlanBusAdapter(mBusRouteResult);
                    //公交车详线路情
                    mBusAdapter.setOnItemClickListener((view, busPath, busRouteResult, position) -> {
                        BusRouteDetailActivity.newInstance(RoutePlanActivity.this,busPath,busRouteResult);
                    });
                    mRecycler.setAdapter(mBusAdapter);

                } else if (result != null && result.getPaths().size() == 0) {
                    Toast.makeText(MyApplication.applicationContext,"~~(>_<)~~ 没有搜索到相关数据",Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MyApplication.applicationContext,"~~(>_<)~~ 没有搜索到相关数据",Toast.LENGTH_SHORT).show();

            }
        }else {


        }
    }

    @Override//驾车路线搜索结果回调
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int rCode) {
        mProgressBar.setVisibility(View.GONE);
        mAMap.clear();
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (driveRouteResult != null && driveRouteResult.getPaths() != null) {
                if (driveRouteResult.getPaths().size() > 0) {
                    mDriveRouteResult = driveRouteResult;
                    final DrivePath drivePath = mDriveRouteResult.getPaths().get(0);
                    DrivingRouteOverlay drivingRouteOverlay = new DrivingRouteOverlay(
                            MyApplication.applicationContext, mAMap, drivePath,
                            mDriveRouteResult.getStartPos(),
                            mDriveRouteResult.getTargetPos(), null);
                    drivingRouteOverlay.setNodeIconVisibility(false);//设置节点marker是否显示
                    drivingRouteOverlay.setIsColorfulline(true);//是否用颜色展示交通拥堵情况，默认true
                    drivingRouteOverlay.removeFromMap();
                    drivingRouteOverlay.addToMap();
                    drivingRouteOverlay.zoomToSpan();
                    int dis = (int) drivePath.getDistance();
                    int dur = (int) drivePath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur)+"("+AMapUtil.getFriendlyLength(dis)+")";
                    mFirstLine.setText(des);
                    int taxiCost = (int) mDriveRouteResult.getTaxiCost();
                    if (taxiCost != 0) {
                        mSeconLine.setVisibility(View.VISIBLE);
                        mSeconLine.setText("打车约" + taxiCost + "元");
                    }
                    mBottomInfo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DriveRouteDetailActivity.newInstance(RoutePlanActivity.this,drivePath,mDriveRouteResult);
                        }
                    });
                } else if (driveRouteResult != null && driveRouteResult.getPaths() == null) {
                    Toast.makeText(MyApplication.applicationContext,"对不起，没有搜索到相关数据",Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(MyApplication.applicationContext,"对不起，没有搜索到相关数据",Toast.LENGTH_SHORT).show();
            }
        } else {
        }
    }

    @Override//步行路线搜索结果回调
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int rCode) {
        mProgressBar.setVisibility(View.GONE);
        mAMap.clear();
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (walkRouteResult != null && walkRouteResult.getPaths() != null) {
                if (walkRouteResult.getPaths().size() > 0) {
                    mWalkRouteResult = walkRouteResult;
                    final WalkPath walkPath = mWalkRouteResult.getPaths()
                            .get(0);
                    WalkRouteOverlay walkRouteOverlay = new WalkRouteOverlay(
                            this, mAMap, walkPath,
                            mWalkRouteResult.getStartPos(),
                            mWalkRouteResult.getTargetPos());
                    walkRouteOverlay.removeFromMap();
                    walkRouteOverlay.addToMap();
                    walkRouteOverlay.zoomToSpan();
                    int dis = (int) walkPath.getDistance();
                    int dur = (int) walkPath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur)+"("+AMapUtil.getFriendlyLength(dis)+")";
                    mFirstLine.setText(des);
                    mSeconLine.setVisibility(View.GONE);
                    mBottomInfo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            WalkRouteDetailActivity.newInstance(RoutePlanActivity.this,walkPath,mWalkRouteResult);
                        }
                    });
                } else if (walkRouteResult != null && walkRouteResult.getPaths() == null) {
                    Toast.makeText(MyApplication.applicationContext,"对不起，没有搜索到相关数据",Toast.LENGTH_SHORT).show();

                }

            } else {
                Toast.makeText(MyApplication.applicationContext,"对不起，没有搜索到相关数据",Toast.LENGTH_SHORT).show();

            }
        } else {
        }
    }

    @Override//骑行路线搜索结果回调
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int rCode) {
        mProgressBar.setVisibility(View.GONE);
        mAMap.clear();
        if (rCode == AMapException.CODE_AMAP_SUCCESS) {
            if (rideRouteResult != null && rideRouteResult.getPaths() != null) {
                if (rideRouteResult.getPaths().size() > 0) {
                    mRideRouteResult = rideRouteResult;
                    final RidePath ridePath = mRideRouteResult.getPaths()
                            .get(0);
                    RideRouteOverlay rideRouteOverlay = new RideRouteOverlay(
                            this, mAMap, ridePath,
                            mRideRouteResult.getStartPos(),
                            mRideRouteResult.getTargetPos());
                    rideRouteOverlay.removeFromMap();
                    rideRouteOverlay.addToMap();
                    rideRouteOverlay.zoomToSpan();
                    int dis = (int) ridePath.getDistance();
                    int dur = (int) ridePath.getDuration();
                    String des = AMapUtil.getFriendlyTime(dur)+"("+AMapUtil.getFriendlyLength(dis)+")";
                    mFirstLine.setText(des);
                    mSeconLine.setVisibility(View.GONE);
                    mBottomInfo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RideRouteDetailActivity.newInstance(RoutePlanActivity.this,ridePath);
                        }
                    });
                } else if (rideRouteResult != null && rideRouteResult.getPaths() == null) {
                    Toast.makeText(MyApplication.applicationContext,"对不起，没有搜索到相关数据",Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MyApplication.applicationContext,"对不起，没有搜索到相关数据",Toast.LENGTH_SHORT).show();
            }
        } else {
         }
    }
}
