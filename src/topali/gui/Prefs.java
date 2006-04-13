// (C) 2003-2006 Iain Milne
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.*;

import topali.mod.*;

import doe.*;

public class Prefs extends PreferencesXML
{
	// Variables not definable by the user...
	public static Locale locale = Locale.getDefault();
		
	public static DecimalFormat d1 = new DecimalFormat("0.0");
	public static DecimalFormat d2 = new DecimalFormat("0.00");
	public static DecimalFormat d4 = new DecimalFormat("0.0000");
	public static DecimalFormat d5 = new DecimalFormat("0.00000");
	public static DecimalFormat i2 = new DecimalFormat("00");
	public static DecimalFormat i3 = new DecimalFormat("000");
	public static DecimalFormat i4 = new DecimalFormat("0000");
	
	public static Font labelFont = (Font) UIManager.get("Label.font");
	
	public static File tmpDir = new File(System.getProperty("java.io.tmpdir"),
//	public static File tmpDir = new File("R:\\",
		System.getProperty("user.name") + "-topaliv2");
	
	public static boolean isWindows =
		System.getProperty("os.name").startsWith("Windows");
	public static boolean isMacOSX = System.getProperty(
		"os.name").startsWith("Mac OS");
	
	// All other variables...
	public static LinkedList<String> gui_recent = new LinkedList<String>();
	public static String gui_dir = System.getProperty("user.home");
	public static boolean gui_first_run = true;
	public static boolean gui_maximized = false;
	public static boolean gui_toolbar_visible = true;
	public static boolean gui_statusbar_visible = true;
	public static boolean gui_tips_visible = true;
	public static int gui_win_width = 800;
	public static int gui_win_height = 600;
	public static int gui_splits_loc = 150;
	public static String gui_find_name = "";
	public static boolean gui_find_highlight = false;
	public static boolean gui_find_case = false;
	public static int gui_filter_tree = Filters.PNG;
	public static int gui_filter_algn = Filters.FAS;
	public static int gui_filter_graph = Filters.CSV;
	public static int gui_goto_nuc = 1;
	public static int gui_project_count = 1;
	public static boolean gui_menu_icons = true;
	public static int gui_movie_x = -1;
	public static int gui_movie_y = -1;
	public static int gui_movie_width = 550;
	public static int gui_movie_height = 450;
	public static boolean gui_movie_current = false;
	public static boolean gui_movie_circular = false;
	public static int gui_movie_window = 500;
	public static int gui_movie_step = 10;
	public static int gui_movie_delay = 25;
	public static int gui_pdialog_x = -1;
	public static int gui_pdialog_y = -1;
	public static boolean gui_preview_current = true;
	public static int gui_pdialog_splitter = -1;
	public static boolean gui_export_allseqs = true;
	public static boolean gui_export_allpars = true;
	public static boolean gui_export_todisk = true;
	public static int gui_odialog_x = -1;
	public static int gui_odialog_y = -1;
	public static int gui_odialog_w = 350;
	public static int gui_odialog_h = 150;
	public static int gui_auto_min = 75;
	public static boolean gui_auto_discard = false;
	public static int gui_tree_method = 0;
	public static boolean gui_tree_useall = true;
	public static float gui_group_threshold = 0.05f;
	
	// Display-initialized variables
	public static boolean gui_seq_tooltip;
	public static byte gui_seq_font_size;
	public static boolean gui_seq_font_bold;
	public static boolean gui_seq_show_text;
	public static boolean gui_seq_show_colors;
	public static boolean gui_graph_smooth;
	public static boolean gui_graph_line;
	public static boolean gui_tree_unique_cols;
	public static boolean gui_seq_dim;
	public static int gui_color_seed = 0;
	public static Color	gui_seq_color_text;
	public static Color	gui_seq_color_a;
	public static Color	gui_seq_color_c;
	public static Color	gui_seq_color_g;
	public static Color	gui_seq_color_t;
	public static Color	gui_seq_color_gpst;
	public static Color	gui_seq_color_hkr;
	public static Color	gui_seq_color_fwy;
	public static Color	gui_seq_color_ilmv;
	public static Color	gui_seq_color_gaps;
	public static Color	gui_graph_window;
	public static Color	gui_graph_threshold;
	public static Color	gui_graph_background;
	public static Color	gui_histo_background;
	public static Color	gui_cardle_line;
	
	// PDM analysis run settings
	public static int pdm_window;
	public static int pdm_step;
	public static boolean pdm_prune;
	public static float pdm_cutoff;
	public static int pdm_seed;
	public static int pdm_burn;
	public static int pdm_cycles;
	public static String pdm_burn_algorithm;
	public static String pdm_main_algorithm;
	public static String pdm_use_beta;
	public static int pdm_parameter_update_interval;
	public static String pdm_update_theta;
	public static int pdm_tune_interval;
	public static String pdm_molecular_clock;
	public static String pdm_category_list;
	public static String pdm_initial_theta;
	public static int pdm_outgroup;
	public static float pdm_global_tune;
	public static float pdm_local_tune;
	public static float pdm_theta_tune;
	public static float pdm_beta_tune;
	
	// HMM analysis run settings
	public static String hmm_model;
	public static String hmm_initial;
	public static float hmm_freq_est_1;
	public static float hmm_freq_est_2;
	public static float hmm_freq_est_3;
	public static float hmm_freq_est_4;
	public static String hmm_transition;
	public static float hmm_transition_ratio;
	public static float hmm_freq_1;
	public static float hmm_freq_2;
	public static float hmm_freq_3;
	public static float hmm_difficulty;
	public static int hmm_burn;
	public static int hmm_points;
	public static int hmm_thinning;
	public static int hmm_tuning;
	public static String hmm_lambda;
	public static String hmm_annealing;
	public static String hmm_station;
	public static String hmm_update;
	public static float hmm_branch;
	
	// DSS analysis run settings
	public static int dss_window, dss_step, dss_runs;
	public static int dss_power, dss_method, dss_pass_count;
	
	// LRT analysis run settings
	public static int lrt_window, lrt_step, lrt_runs, lrt_method;
	
	// Vamsas/web settings
	public static String web_topali_url;
	public static int web_check_secs;
	public static boolean web_check_startup;
	public static boolean web_proxy_enable;
	public static String web_proxy_server;
	public static int web_proxy_port;
	public static String web_proxy_username, web_proxy_password;
	
	protected void getPreferences()
	{
		setDisplayDefaults();
		setPDMDefaults();
		setHMMDefaults();
		setDSSDefaults();
		setLRTDefaults();
		setWebDefaults();
		
		System.out.println("window is " + pdm_window);
		
		for (int i = 0; i < 4; i++)
		{
			String str = getStr("gui_recent_" + i, "");
			if (str.length() > 0)
				gui_recent.add(str);
		}
		
		gui_dir = getStr("gui_dir", gui_dir);
		gui_first_run = getBool("gui_first_run", gui_first_run);
		gui_maximized = getBool("gui_maximized", gui_maximized);
		gui_toolbar_visible = getBool("gui_toolbar_visible", gui_toolbar_visible);
		gui_statusbar_visible = getBool("gui_statusbar_visible", gui_statusbar_visible);
		gui_tips_visible = getBool("gui_tips_visible", gui_tips_visible);
		gui_win_width = getInt("gui_win_width", gui_win_width);
		gui_win_height = getInt("gui_win_height", gui_win_height);
		gui_splits_loc = getInt("gui_splits_loc", gui_splits_loc);
		gui_find_name = getStr("gui_find_name", gui_find_name);
		gui_find_highlight = getBool("gui_find_highlight", gui_find_highlight);
		gui_find_case = getBool("gui_find_case", gui_find_case);
		gui_filter_tree = getInt("gui_filter_tree", gui_filter_tree);
		gui_filter_algn = getInt("gui_filter_algn", gui_filter_algn);
		gui_filter_graph = getInt("gui_filter_graph", gui_filter_graph);
		gui_goto_nuc = getInt("gui_goto_nuc", gui_goto_nuc);
		gui_project_count = getInt("gui_project_count", gui_project_count);
		gui_menu_icons = getBool("gui_menu_icons", gui_menu_icons);
		gui_movie_x = getInt("gui_movie_x", gui_movie_x);
		gui_movie_y = getInt("gui_movie_y", gui_movie_y);
		gui_movie_width = getInt("gui_movie_width", gui_movie_width);
		gui_movie_height = getInt("gui_movie_height", gui_movie_height);
		gui_movie_current = getBool("gui_movie_current", gui_movie_current);
		gui_movie_circular = getBool("gui_movie_circular", gui_movie_circular);
		gui_movie_window = getInt("gui_movie_window", gui_movie_window);
		gui_movie_step = getInt("gui_movie_step", gui_movie_step);
		gui_movie_delay = getInt("gui_movie_delay", gui_movie_delay);
		gui_pdialog_x = getInt("gui_pdialog_x", gui_pdialog_x);
		gui_pdialog_y = getInt("gui_pdialog_y", gui_pdialog_y);
		gui_preview_current = getBool("gui_preview_current", gui_preview_current);
		gui_pdialog_splitter = getInt("gui_pdialog_splitter", gui_pdialog_splitter);
		gui_export_allseqs = getBool("gui_export_allseqs", gui_export_allseqs);
		gui_export_allpars = getBool("gui_export_allpars", gui_export_allpars);
		gui_export_todisk = getBool("gui_export_todisk", gui_export_todisk);
		gui_odialog_x = getInt("gui_odialog_x", gui_odialog_x);
		gui_odialog_y = getInt("gui_odialog_y", gui_odialog_y);
		gui_odialog_w = getInt("gui_odialog_w", gui_odialog_w);
		gui_odialog_h = getInt("gui_odialog_h", gui_odialog_h);
		gui_auto_min = getInt("gui_auto_min", gui_auto_min);
		gui_auto_discard = getBool("gui_auto_discard", gui_auto_discard);
		gui_tree_method = getInt("gui_tree_method", gui_tree_method);
		gui_tree_useall = getBool("gui_tree_useall", gui_tree_useall);
		gui_group_threshold = getFloat("gui_group_threshold", gui_group_threshold);
		
		gui_seq_tooltip = getBool("gui_seq_tooltip", gui_seq_tooltip);
		gui_seq_font_size = getByte("gui_seq_font_size", gui_seq_font_size);
		gui_seq_font_bold = getBool("gui_seq_font_bold", gui_seq_font_bold);
		gui_seq_show_text = getBool("gui_seq_show_text", gui_seq_show_text);
		gui_seq_show_colors = getBool("gui_seq_show_colors", gui_seq_show_colors);
		gui_graph_smooth = getBool("gui_graph_smooth", gui_graph_smooth);
		gui_graph_line = getBool("gui_graph_line", gui_graph_line);
		gui_seq_dim = getBool("gui_seq_dim", gui_seq_dim);
		gui_tree_unique_cols = getBool("gui_tree_unique_cols", gui_tree_unique_cols);
		gui_color_seed = getInt("gui_color_seed", gui_color_seed);
		gui_seq_color_text = getColor("gui_seq_color_text", gui_seq_color_text);
		gui_seq_color_a = getColor("gui_seq_color_a", gui_seq_color_a);
		gui_seq_color_c = getColor("gui_seq_color_c", gui_seq_color_c);
		gui_seq_color_g = getColor("gui_seq_color_g", gui_seq_color_g);
		gui_seq_color_t = getColor("gui_seq_color_t", gui_seq_color_t);
		gui_seq_color_gpst = getColor("gui_seq_color_gpst", gui_seq_color_gpst);
		gui_seq_color_hkr = getColor("gui_seq_color_hkr", gui_seq_color_hkr);
		gui_seq_color_fwy = getColor("gui_seq_color_fwy", gui_seq_color_fwy);
		gui_seq_color_ilmv = getColor("gui_seq_color_ilmv", gui_seq_color_ilmv);
		gui_seq_color_gaps = getColor("gui_seq_color_gaps", gui_seq_color_gaps);
		gui_graph_window = getColor("gui_graph_window", gui_graph_window);
		gui_graph_threshold = getColor("gui_graph_threshold", gui_graph_threshold);
		gui_graph_background = getColor("gui_graph_background", gui_graph_background);
		gui_histo_background = getColor("gui_histo_background", gui_histo_background);
		gui_cardle_line = getColor("gui_cardle_line", gui_cardle_line);
		
		pdm_window = getInt("pdm_window", pdm_window);
		pdm_step = getInt("pdm_step", pdm_step);
		pdm_prune = getBool("pdm_prune", pdm_prune);
		pdm_cutoff = getFloat("pdm_cutoff", pdm_cutoff);
		pdm_seed = getInt("pdm_seed", pdm_seed);
		pdm_burn = getInt("pdm_burn", pdm_burn);
		pdm_cycles = getInt("pdm_cycles", pdm_cycles);
		pdm_burn_algorithm = getStr("pdm_burn_algorithm", pdm_burn_algorithm);
		pdm_main_algorithm = getStr("pdm_main_algorithm", pdm_main_algorithm);
		pdm_use_beta = getStr("pdm_use_beta", pdm_use_beta);
		pdm_parameter_update_interval = getInt("pdm_parameter_update_interval", pdm_parameter_update_interval);
		pdm_update_theta = getStr("pdm_update_theta", pdm_update_theta);
		pdm_tune_interval = getInt("pdm_tune_interval", pdm_tune_interval);
		pdm_molecular_clock = getStr("pdm_molecular_clock", pdm_molecular_clock);
		pdm_category_list = getStr("pdm_category_list", pdm_category_list);
		pdm_initial_theta = getStr("pdm_initial_theta", pdm_initial_theta);
		pdm_outgroup = getInt("pdm_outgroup", pdm_outgroup);
		pdm_global_tune = getFloat("pdm_global_tune", pdm_global_tune);
		pdm_local_tune = getFloat("pdm_local_tune", pdm_local_tune);
		pdm_theta_tune = getFloat("pdm_theta_tune", pdm_theta_tune);
		pdm_beta_tune = getFloat("pdm_beta_tune", pdm_beta_tune);
		
		hmm_model = getStr("hmm_model", hmm_model);
		hmm_initial = getStr("hmm_initial", hmm_initial);
		hmm_freq_est_1 = getFloat("hmm_freq_est_1", hmm_freq_est_1);
		hmm_freq_est_2 = getFloat("hmm_freq_est_2", hmm_freq_est_2);
		hmm_freq_est_3 = getFloat("hmm_freq_est_3", hmm_freq_est_3);
		hmm_freq_est_4 = getFloat("hmm_freq_est_4", hmm_freq_est_4);
		hmm_transition = getStr("hmm_transition", hmm_transition);
		hmm_transition_ratio = getFloat("hmm_transition_ratio", hmm_transition_ratio);
		hmm_freq_1 = getFloat("hmm_freq_1", hmm_freq_1);
		hmm_freq_2 = getFloat("hmm_freq_2", hmm_freq_2);
		hmm_freq_3 = getFloat("hmm_freq_3", hmm_freq_3);
		hmm_difficulty = getFloat("hmm_difficulty", hmm_difficulty);
		hmm_burn = getInt("hmm_burn", hmm_burn);
		hmm_points = getInt("hmm_points", hmm_points);
		hmm_thinning = getInt("hmm_thinning", hmm_thinning);
		hmm_tuning = getInt("hmm_tuning", hmm_tuning);
		hmm_lambda = getStr("hmm_lambda", hmm_lambda);
		hmm_annealing = getStr("hmm_annealing", hmm_annealing);
		hmm_station = getStr("hmm_station", hmm_station);
		hmm_update = getStr("hmm_update", hmm_update);
		hmm_branch = getFloat("hmm_branch", hmm_branch);
		
		dss_window = getInt("dss_window", dss_window);
		dss_step = getInt("dss_step", dss_step);
		dss_runs = getInt("dss_runs", dss_runs);
		dss_power = getInt("dss_power", dss_power);
		dss_method = getInt("dss_method", dss_method);
		dss_pass_count = getInt("dss_pass_count", dss_pass_count);
		
		lrt_window = getInt("lrt_window", lrt_window);
		lrt_step = getInt("lrt_step", lrt_step);
		lrt_runs = getInt("lrt_runs", lrt_runs);
		lrt_method = getInt("lrt_method", lrt_method);
		
		web_topali_url = getStr("web_topali_url", web_topali_url);
		web_check_secs = getInt("web_check_secs", web_check_secs);
		web_check_startup = getBool("web_check_startup", web_check_startup);
		web_proxy_enable = getBool("web_proxy_enable", web_proxy_enable);
		web_proxy_server = getStr("web_proxy_server", web_proxy_server);
		web_proxy_port = getInt("web_proxy_port", web_proxy_port);
		web_proxy_username = getStr("web_proxy_username", web_proxy_username);
		web_proxy_password = getStr("web_proxy_password", web_proxy_password);
	}
	
	protected void setPreferences()
	{
		for (int i = 0; i < gui_recent.size(); i++)
			p.setProperty("gui_recent_" + i, gui_recent.get(i));
		
		setStr("gui_dir", gui_dir);
		p.setProperty("gui_first_run", "" + gui_first_run);
		p.setProperty("gui_maximized", "" + gui_maximized);
		p.setProperty("gui_toolbar_visible", "" + gui_toolbar_visible);
		p.setProperty("gui_statusbar_visible", "" + gui_statusbar_visible);
		p.setProperty("gui_tips_visible", "" + gui_tips_visible);
		p.setProperty("gui_win_width", "" + gui_win_width);
		p.setProperty("gui_win_height", "" + gui_win_height);
		p.setProperty("gui_splits_loc", "" + gui_splits_loc);
		setStr("gui_find_name", gui_find_name);
		p.setProperty("gui_find_highlight", "" + gui_find_highlight);
		p.setProperty("gui_find_case", "" + gui_find_case);
		p.setProperty("gui_filter_tree", "" + gui_filter_tree);
		p.setProperty("gui_filter_algn", "" + gui_filter_algn);
		p.setProperty("gui_filter_graph", "" + gui_filter_graph);
		p.setProperty("gui_goto_nuc", "" + gui_goto_nuc);
		p.setProperty("gui_project_count", "" + gui_project_count);
		p.setProperty("gui_menu_icons", "" + gui_menu_icons);
		p.setProperty("gui_movie_x", "" + gui_movie_x);
		p.setProperty("gui_movie_y", "" + gui_movie_y);
		p.setProperty("gui_movie_width", "" + gui_movie_width);
		p.setProperty("gui_movie_height", "" + gui_movie_height);
		p.setProperty("gui_movie_current", "" + gui_movie_current);
		p.setProperty("gui_movie_circular", "" + gui_movie_circular);
		p.setProperty("gui_movie_window", "" + gui_movie_window);
		p.setProperty("gui_movie_step", "" + gui_movie_step);
		p.setProperty("gui_movie_delay", "" + gui_movie_delay);
		p.setProperty("gui_pdialog_x", "" + gui_pdialog_x);
		p.setProperty("gui_pdialog_y", "" + gui_pdialog_y);
		p.setProperty("gui_preview_current", "" + gui_preview_current);
		p.setProperty("gui_pdialog_splitter", "" + gui_pdialog_splitter);
		p.setProperty("gui_export_allseqs", "" + gui_export_allseqs);
		p.setProperty("gui_export_allpars", "" + gui_export_allpars);
		p.setProperty("gui_export_todisk", "" + gui_export_todisk);
		p.setProperty("gui_odialog_x", "" + gui_odialog_x);
		p.setProperty("gui_odialog_y", "" + gui_odialog_y);
		p.setProperty("gui_odialog_w", "" + gui_odialog_w);
		p.setProperty("gui_odialog_h", "" + gui_odialog_h);
		p.setProperty("gui_auto_min", "" + gui_auto_min);
		p.setProperty("gui_auto_discard", "" + gui_auto_discard);
		p.setProperty("gui_tree_method", "" + gui_tree_method);
		p.setProperty("gui_tree_useall", "" + gui_tree_useall);
		p.setProperty("gui_group_threshold", "" + gui_group_threshold);
		
		p.setProperty("gui_seq_tooltip", "" + gui_seq_tooltip);
		p.setProperty("gui_seq_font_size", "" + gui_seq_font_size);
		p.setProperty("gui_seq_font_bold", "" + gui_seq_font_bold);
		p.setProperty("gui_seq_show_text", "" + gui_seq_show_text);
		p.setProperty("gui_seq_show_colors", "" + gui_seq_show_colors);
		p.setProperty("gui_graph_smooth", "" + gui_graph_smooth);
		p.setProperty("gui_graph_line", "" + gui_graph_line);
		p.setProperty("gui_seq_dim", "" + gui_seq_dim);
		p.setProperty("gui_tree_unique_cols", "" + gui_tree_unique_cols);
		p.setProperty("gui_color_seed", "" + gui_color_seed);
		p.setProperty("gui_seq_color_text", setColor(gui_seq_color_text));
		p.setProperty("gui_seq_color_a", setColor(gui_seq_color_a));
		p.setProperty("gui_seq_color_c", setColor(gui_seq_color_c));
		p.setProperty("gui_seq_color_g", setColor(gui_seq_color_g));
		p.setProperty("gui_seq_color_t", setColor(gui_seq_color_t));
		p.setProperty("gui_seq_color_gpst", setColor(gui_seq_color_gpst));
		p.setProperty("gui_seq_color_hkr", setColor(gui_seq_color_hkr));
		p.setProperty("gui_seq_color_fwy", setColor(gui_seq_color_fwy));
		p.setProperty("gui_seq_color_ilmv", setColor(gui_seq_color_ilmv));
		p.setProperty("gui_seq_color_gaps", setColor(gui_seq_color_gaps));
		p.setProperty("gui_graph_window", setColor(gui_graph_window));
		p.setProperty("gui_graph_threshold", setColor(gui_graph_threshold));
		p.setProperty("gui_graph_background", setColor(gui_graph_background));
		p.setProperty("gui_histo_background", setColor(gui_histo_background));
		p.setProperty("gui_cardle_line", setColor(gui_cardle_line));
		
		p.setProperty("pdm_window", "" + pdm_window);
		p.setProperty("pdm_step", "" + pdm_step);
		p.setProperty("pdm_prune", "" + pdm_prune);
		p.setProperty("pdm_cutoff", "" + pdm_cutoff);
		p.setProperty("pdm_seed", "" + pdm_seed);
		p.setProperty("pdm_burn", "" + pdm_burn);
		p.setProperty("pdm_cycles", "" + pdm_cycles);
		setStr("pdm_burn_algorithm", pdm_burn_algorithm);
		setStr("pdm_main_algorithm", pdm_main_algorithm);
		p.setProperty("pdm_use_beta", "" + pdm_use_beta);
		p.setProperty("pdm_parameter_update_interval", "" + pdm_parameter_update_interval);
		p.setProperty("pdm_update_theta", "" + pdm_update_theta);
		p.setProperty("pdm_tune_interval", "" + pdm_tune_interval);			
		p.setProperty("pdm_molecular_clock", "" + pdm_molecular_clock);
		setStr("pdm_category_list", pdm_category_list);
		setStr("pdm_initial_theta", pdm_initial_theta);
		p.setProperty("pdm_outgroup", "" + pdm_outgroup);
		p.setProperty("pdm_global_tune", "" + pdm_global_tune);
		p.setProperty("pdm_local_tune", "" + pdm_local_tune);
		p.setProperty("pdm_theta_tune", "" + pdm_theta_tune);
		p.setProperty("pdm_beta_tune", "" + pdm_beta_tune);
		
		setStr("hmm_model", hmm_model);
		setStr("hmm_initial", hmm_initial);
		p.setProperty("hmm_freq_est_1", "" + hmm_freq_est_1);
		p.setProperty("hmm_freq_est_2", "" + hmm_freq_est_2);
		p.setProperty("hmm_freq_est_3", "" + hmm_freq_est_3);
		p.setProperty("hmm_freq_est_4", "" + hmm_freq_est_4);
		setStr("hmm_transition", hmm_transition);
		p.setProperty("hmm_transition_ratio", "" + hmm_transition_ratio);
		p.setProperty("hmm_freq_1", "" + hmm_freq_1);
		p.setProperty("hmm_freq_2", "" + hmm_freq_2);
		p.setProperty("hmm_freq_3", "" + hmm_freq_3);
		p.setProperty("hmm_difficulty", "" + hmm_difficulty);
		p.setProperty("hmm_burn", "" + hmm_burn);
		p.setProperty("hmm_points", "" + hmm_points);
		p.setProperty("hmm_thinning", "" + hmm_thinning);
		p.setProperty("hmm_tuning", "" + hmm_tuning);
		setStr("hmm_lambda", hmm_lambda);
		setStr("hmm_annealing", hmm_annealing);
		setStr("hmm_station", hmm_station);
		setStr("hmm_update", hmm_update);
		p.setProperty("hmm_branch", "" + hmm_branch);
		
		p.setProperty("dss_window", "" + dss_window);
		p.setProperty("dss_step", "" + dss_step);
		p.setProperty("dss_runs", "" + dss_runs);
		p.setProperty("dss_power", "" + dss_power);
		p.setProperty("dss_method", "" + dss_method);
		p.setProperty("dss_pass_count", "" + dss_pass_count);
		
		p.setProperty("lrt_window", "" + lrt_window);
		p.setProperty("lrt_step", "" + lrt_step);
		p.setProperty("lrt_runs", "" + lrt_runs);
		p.setProperty("lrt_method", "" + lrt_method);
		
		setStr("web_topali_url", web_topali_url);
		p.setProperty("web_check_secs", "" + web_check_secs);
		p.setProperty("web_check_startup", "" + web_check_startup);
		p.setProperty("web_proxy_enable", "" + web_proxy_enable);
		setStr("web_proxy_server", web_proxy_server);
		p.setProperty("web_proxy_port", "" + web_proxy_port);
		setStr("web_proxy_username", web_proxy_username);
		setStr("web_proxy_password", web_proxy_password);
	}
	
	public static void setDisplayDefaults()
	{
		gui_seq_tooltip = false;
		gui_seq_font_size = 12;
		gui_seq_font_bold = false;
		gui_seq_show_text = true;
		gui_seq_show_colors = true;
		gui_graph_smooth = false;
		gui_graph_line = true;
		gui_seq_dim = true;
		gui_tree_unique_cols = true;
		gui_color_seed = 0;
		gui_seq_color_text = new Color(0, 0, 0);
		gui_seq_color_a = new Color(153, 255, 153);
		gui_seq_color_c = new Color(255, 204, 153);
		gui_seq_color_g = new Color(255, 153, 153);
		gui_seq_color_t = new Color(153, 153, 255);
		gui_seq_color_gpst = new Color(255, 204, 153);
		gui_seq_color_hkr = new Color(255, 153, 153);
		gui_seq_color_fwy = new Color(153, 153, 255);
		gui_seq_color_ilmv = new Color(153, 255, 153);
		gui_seq_color_gaps = new Color(255, 255, 255);
		gui_graph_window = new Color(0, 0, 255);
		gui_graph_threshold = new Color(0, 255, 64);
		gui_graph_background = new Color(255, 255, 255);
		gui_histo_background = new Color(255, 255, 255);
		gui_cardle_line = new Color(0, 0, 0);
	}
	
	public static void setPDMDefaults()
	{
		pdm_window = 500;
		pdm_step = 10;
		pdm_prune = true;
		pdm_cutoff = 0.05f;
		pdm_seed = 194024933;
		pdm_burn = 1000;
		pdm_cycles = 6000;
		pdm_burn_algorithm = "global";
		pdm_main_algorithm = "local";
		pdm_use_beta = "false";
		pdm_parameter_update_interval = 1;
		pdm_update_theta = "true";
		pdm_tune_interval = 200;	
		pdm_molecular_clock = "false";
		pdm_category_list = "1*";
		pdm_initial_theta = d5.format(1.0);
		pdm_outgroup = 1;
		pdm_global_tune = 0.01f;
		pdm_local_tune = 0.19f;
		pdm_theta_tune = 2000.0f;
		pdm_beta_tune = 10.0f;
	}
	
	public static void setHMMDefaults()
	{
		hmm_model = "F84+gaps";
		hmm_initial = "Yes";
		hmm_freq_est_1 = 0.25f;
		hmm_freq_est_2 = 0.25f;
		hmm_freq_est_3 = 0.25f;
		hmm_freq_est_4 = 0.25f;
		hmm_transition = "Yes";
		hmm_transition_ratio = 0.2f;
		hmm_freq_1 = 0.333f;
		hmm_freq_2 = 0.333f;
		hmm_freq_3 = 0.333f;
		hmm_difficulty = 0.9f;
		hmm_burn = 10000;
		hmm_points = 1000;
		hmm_thinning = 10;
		hmm_tuning = 100;
		hmm_lambda = "Yes";
		hmm_annealing = "NONE";
		hmm_station = "Yes";
		hmm_update = "Yes";
		hmm_branch = 0.1f;
	}
	
	public static void setDSSDefaults()
	{
		dss_window     = 500;
		dss_step       = 10;
		dss_runs       = 100;
		dss_power      = topali.cluster.dss.DSS.POWER_UNWEIGHTED;
		dss_method     = topali.cluster.dss.DSS.METHOD_JC;
		dss_pass_count = topali.cluster.dss.DSS.ONE_PASS;
	}
	
	public static void setLRTDefaults()
	{
		lrt_window = 500;
		lrt_step = 10;
		lrt_runs = 100;
		lrt_method = topali.cluster.lrt.LRT.METHOD_JC;
	}
	
	public static void setWebDefaults()
	{
		web_topali_url = "http://www.compbio.dundee.ac.uk/topali";
		web_check_secs = 30;
		web_check_startup = false;
		web_proxy_port = 8080;
		web_proxy_server = "";
		web_proxy_username = "";
		web_proxy_password = "";
	}
}