package org.navitproject.glassheadup;

import java.util.HashMap;

public class NavImages {

    private static HashMap<Integer, String> navimages = new HashMap<Integer, String>() {{
        put(65969, "nav_straight");
        put(65971, "nav_right_1");
        put(65972, "nav_right_2");
        put(65973, "nav_right_3");
        put(65974, "nav_left_1");
        put(65975, "nav_left_2");
        put(65976, "nav_left_3");
        put(65977, "nav_roundabout_r1");
        put(65978, "nav_roundabout_r2");
        put(65979, "nav_roundabout_r3");
        put(65980, "nav_roundabout_r4");
        put(65981, "nav_roundabout_r5");
        put(65982, "nav_roundabout_r6");
        put(65983, "nav_roundabout_r7");
        put(65984, "nav_roundabout_r8");
        put(65985, "nav_roundabout_l1");
        put(65986, "nav_roundabout_l2");
        put(65987, "nav_roundabout_l3");
        put(65988, "nav_roundabout_l4");
        put(65989, "nav_roundabout_l5");
        put(65990, "nav_roundabout_l6");
        put(65991, "nav_roundabout_l7");
        put(65992, "nav_roundabout_l8");
        put(66018, "nav_destination");
        put(66092, "nav_merge_left");
        put(66093, "nav_merge_right");
        put(66094, "nav_turnaround_left");
        put(66095, "nav_turnaround_right");
        put(66096, "nav_exit_left");
        put(66097, "nav_exit_right");
        put(66098, "nav_keep_left");
        put(66099, "nav_keep_right");
    }};
    private static HashMap<Integer, String> navstatusimages = new HashMap<Integer, String>() {{
        put(-2, "status_no_destination");
        put(-1, "status_no_route");
        put(0, "status_no_destination");
        put(1, "status_position_wait");
        put(2, "status_calculating");
        put(3, "status_recalculating");
        put(4, "status_routing");
    }};
    private static HashMap<Integer, String> gpssignalstrengthimages = new HashMap<Integer, String>() {{
        put(0, "gui_strength_0");
        put(1, "gui_strength_1");
        put(2, "gui_strength_2");
        put(3, "gui_strength_3");
        put(4, "gui_strength_4");
        put(5, "gui_strength_5");
    }};

    public static HashMap<Integer, String> getNavstatusimages() {
        return navstatusimages;
    }

    public static HashMap<Integer, String> getNavImages() {
        return navimages;
    }

    public static HashMap<Integer, String> getGpssignalstrengthimages() {
        return gpssignalstrengthimages;
    }

}
