// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.gui;

import java.awt.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

import javax.swing.UIManager;

import org.apache.log4j.Logger;

import topali.data.ModelTestResult;
import topali.mod.Filters;
import doe.PreferencesXML;

public class Prefs extends PreferencesXML
{
	public static String gui_recent0;
	public static String gui_recent1;
	public static String gui_recent2;
	public static String gui_recent3;
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
	public static int gui_filter_table = Filters.CSV;
	public static int gui_goto_nuc = 1;
	public static int gui_project_count = 1;
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
	public static int gui_export_pars = 1;
	public static boolean gui_export_todisk = true;
	public static int gui_odialog_x = -1;
	public static int gui_odialog_y = -1;
	public static int gui_odialog_w = 350;
	public static int gui_odialog_h = 150;
	public static int gui_auto_min = 75;
	public static boolean gui_auto_discard = false;
	public static float gui_group_threshold = 0.05f;
	public static int gui_import_method = 0;
	public static int gui_max_cpus = Runtime.getRuntime().availableProcessors();
	public static boolean gui_show_horizontal_highlight = false;
	public static boolean gui_show_vertical_highlight = false;
	
	// Display-initialized variables
	public static boolean gui_seq_tooltip;
	public static byte gui_seq_font_size;
	public static boolean gui_seq_font_bold;
	public static boolean gui_seq_show_text;
	public static boolean gui_seq_antialias;
	public static boolean gui_seq_show_colors;
	public static boolean gui_graph_smooth;
	public static boolean gui_tree_unique_cols;
	public static boolean gui_seq_dim;
	public static int gui_color_seed = 0;
	public static Color gui_seq_color_text;
	public static Color gui_seq_color_a;
	public static Color gui_seq_color_c;
	public static Color gui_seq_color_g;
	public static Color gui_seq_color_t;
	public static Color gui_seq_color_gpst;
	public static Color gui_seq_color_hkr;
	public static Color gui_seq_color_fwy;
	public static Color gui_seq_color_ilmv;
	public static Color gui_seq_color_gaps;
	public static Color gui_seq_highlight;
	public static Color gui_graph_window;
	public static Color gui_graph_threshold;
	public static Color gui_graph_background;
	public static Color gui_graph_color;

	// PDM2 analysis run settings
	public static int pdm2_window;
	public static int pdm2_step;

	// PDM analysis run settings
	public static int pdm_window;
	public static int pdm_step;
	public static int pdm_runs;
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
	public static double dss_gap_threshold;
	
	// LRT analysis run settings
	public static int lrt_window, lrt_step, lrt_runs, lrt_method;
	public static double lrt_gap_threshold;

	// Quick tree settings
	public static int qt_bootstrap;
	public static double qt_tstv;
	public static double qt_alpha;
	public static int qt_bootstrap_default;
	public static double qt_tstv_default;
	public static double qt_alpha_default;
	
	// Mr Bayes run settings
	public static int mb_type;
	public static int mb_runs;
	public static int mb_gens;
	public static int mb_samplefreq;
	public static int mb_burnin;
	public static String mb_default_dnamodel;
	public static String mb_default_proteinmodel;
	public static boolean mb_default_model_gamma;
	public static boolean mb_default_model_inv;
	public static int mb_runs_default;
	public static int mb_gens_default;
	public static int mb_samplefreq_default;
	public static int mb_burnin_default;
	
	// Phyml run settings
	public static int phyml_bootstrap;
	public static int phyml_bootstrap_default;
	public static String phyml_dnamodel_default;
	public static String phyml_proteinmodel_default;
	
	//Raxml run settigns
	public static int rax_type;
	public static String rax_ratehet;
	public static int rax_bootstrap;
	public static boolean rax_empfreq;
	public static String rax_protmodel;
	public static String rax_ratehet_default;
	public static int rax_bootstrap_default;
	public static boolean rax_empfreq_default;
	public static String rax_protmodel_default;
	
	// Model selection run settings
	public static String ms_models;
	public static boolean ms_gamma;
	public static boolean ms_inv;
	public static String ms_samplesize;
	
	// Vamsas/web settings
	public static String web_direct_url;
	public static String web_broker_url;
	public static boolean web_use_rbroker;
	public static int web_check_secs;
	public static boolean web_check_startup;
	public static boolean web_proxy_enable;
	public static String web_proxy_server;
	public static int web_proxy_port;
	public static String web_proxy_username, web_proxy_password;

	// Id for identifying this client
	public static String appId = new String();

	protected void getPreferences()
	{
		// Generate a "unique" 32 character id number
		Random rnd = new Random();
		for (int i = 0; i < 32; i++)
			appId += rnd.nextInt(10);

		setDisplayDefaults();
		setPDMDefaults();
		setPDM2Defaults();
		setHMMDefaults();
		setDSSDefaults();
		setLRTDefaults();
		setQTDefaults();
		setMBDefaults();
		setRaxDefaults();
		setPhymlDefaults();
		setMSDefaults();
		setWebDefaults();

		gui_recent0 = getStr("gui_recent0", gui_recent0);
		gui_recent1 = getStr("gui_recent1", gui_recent1);
		gui_recent2 = getStr("gui_recent2", gui_recent2);
		gui_recent3 = getStr("gui_recent3", gui_recent3);
		gui_dir = getStr("gui_dir", gui_dir);
		gui_first_run = getBool("gui_first_run", gui_first_run);
		gui_maximized = getBool("gui_maximized", gui_maximized);
		gui_toolbar_visible = getBool("gui_toolbar_visible",
				gui_toolbar_visible);
		gui_statusbar_visible = getBool("gui_statusbar_visible",
				gui_statusbar_visible);
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
		gui_filter_table = getInt("gui_filter_table", gui_filter_table);
		gui_goto_nuc = getInt("gui_goto_nuc", gui_goto_nuc);
		gui_project_count = getInt("gui_project_count", gui_project_count);
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
		gui_preview_current = getBool("gui_preview_current",
				gui_preview_current);
		gui_pdialog_splitter = getInt("gui_pdialog_splitter",
				gui_pdialog_splitter);
		gui_export_allseqs = getBool("gui_export_allseqs", gui_export_allseqs);
		gui_export_pars = getInt("gui_export_pars", gui_export_pars);
		gui_export_todisk = getBool("gui_export_todisk", gui_export_todisk);
		gui_odialog_x = getInt("gui_odialog_x", gui_odialog_x);
		gui_odialog_y = getInt("gui_odialog_y", gui_odialog_y);
		gui_odialog_w = getInt("gui_odialog_w", gui_odialog_w);
		gui_odialog_h = getInt("gui_odialog_h", gui_odialog_h);
		gui_auto_min = getInt("gui_auto_min", gui_auto_min);
		gui_auto_discard = getBool("gui_auto_discard", gui_auto_discard);
		gui_group_threshold = getFloat("gui_group_threshold",
				gui_group_threshold);
		gui_import_method = getInt("gui_import_method", gui_import_method);
		gui_max_cpus = getInt("gui_max_cpus", gui_max_cpus);
		
		gui_seq_tooltip = getBool("gui_seq_tooltip", gui_seq_tooltip);
		gui_seq_font_size = getByte("gui_seq_font_size", gui_seq_font_size);
		gui_seq_font_bold = getBool("gui_seq_font_bold", gui_seq_font_bold);
		gui_seq_show_text = getBool("gui_seq_show_text", gui_seq_show_text);
		gui_seq_antialias = getBool("gui_seq_antialias", gui_seq_antialias);
		gui_seq_show_colors = getBool("gui_seq_show_colors",
				gui_seq_show_colors);
		gui_graph_smooth = getBool("gui_graph_smooth", gui_graph_smooth);
		gui_seq_dim = getBool("gui_seq_dim", gui_seq_dim);
		gui_tree_unique_cols = getBool("gui_tree_unique_cols",
				gui_tree_unique_cols);
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
		gui_seq_highlight = getColor("gui_seq_highlight", gui_seq_highlight);
		gui_graph_window = getColor("gui_graph_window", gui_graph_window);
		gui_graph_threshold = getColor("gui_graph_threshold",
				gui_graph_threshold);
		gui_graph_background = getColor("gui_graph_background",
				gui_graph_background);
		gui_graph_color = getColor("gui_graph_color", gui_graph_color);
		
		gui_show_horizontal_highlight = getBool(
				"gui_show_horizontal_highlight", gui_show_horizontal_highlight);
		gui_show_vertical_highlight = getBool("gui_show_vertical_highlight",
				gui_show_vertical_highlight);

		pdm2_window = getInt("pdm2_window", pdm2_window);
		pdm2_step = getInt("pdm2_step", pdm2_step);

		pdm_window = getInt("pdm_window", pdm_window);
		pdm_step = getInt("pdm_step", pdm_step);
		pdm_runs = getInt("pdm_runs", pdm_runs);
		pdm_prune = getBool("pdm_prune", pdm_prune);
		pdm_cutoff = getFloat("pdm_cutoff", pdm_cutoff);
		pdm_seed = getInt("pdm_seed", pdm_seed);
		pdm_burn = getInt("pdm_burn", pdm_burn);
		pdm_cycles = getInt("pdm_cycles", pdm_cycles);
		pdm_burn_algorithm = getStr("pdm_burn_algorithm", pdm_burn_algorithm);
		pdm_main_algorithm = getStr("pdm_main_algorithm", pdm_main_algorithm);
		pdm_use_beta = getStr("pdm_use_beta", pdm_use_beta);
		pdm_parameter_update_interval = getInt("pdm_parameter_update_interval",
				pdm_parameter_update_interval);
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
		hmm_transition_ratio = getFloat("hmm_transition_ratio",
				hmm_transition_ratio);
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
		dss_gap_threshold = getFloat("dss_gap_threshold", (float)dss_gap_threshold);

		lrt_window = getInt("lrt_window", lrt_window);
		lrt_step = getInt("lrt_step", lrt_step);
		lrt_runs = getInt("lrt_runs", lrt_runs);
		lrt_method = getInt("lrt_method", lrt_method);
		lrt_gap_threshold = getFloat("lrt_gap_threshold", (float)lrt_gap_threshold);

		qt_bootstrap = getInt("qt_bootstrap", qt_bootstrap);
		qt_tstv = getFloat("qt_tstv", (float)qt_tstv);
		qt_alpha = getFloat("qt_alpha", (float)qt_alpha);
		
		mb_type = getInt("mb_type", mb_type);
		mb_runs = getInt("mb_runs", mb_runs);
		mb_gens = getInt("mb_gens", mb_gens);
		mb_samplefreq = getInt("mb_samplefreq", mb_samplefreq);
		mb_burnin = getInt("mb_burnin", mb_burnin);

		rax_type = getInt("rax_type", rax_type);
		rax_ratehet = getStr("rax_ratehet", rax_ratehet);
		rax_empfreq = getBool("rax_empfreq", rax_empfreq);
		rax_bootstrap = getInt("rax_bootstrap", rax_bootstrap);
		rax_protmodel = getStr("rax_protmodel", rax_protmodel);
		
		phyml_bootstrap = getInt("phyml_bootstrap", phyml_bootstrap);
		
		ms_models = getStr("ms_models", ms_models);
		ms_gamma = getBool("ms_gamma", ms_gamma);
		ms_inv = getBool("ms_inv", ms_inv);
		ms_samplesize = getStr("ms_samplesize", ms_samplesize);
		
		web_direct_url = getStr("web_direct_url", web_direct_url);
		web_broker_url = getStr("web_broker_url", web_broker_url);
		web_use_rbroker = getBool("web_use_rbroker", web_use_rbroker);
		web_check_secs = getInt("web_check_secs", web_check_secs);
		web_check_startup = getBool("web_check_startup", web_check_startup);
		web_proxy_enable = getBool("web_proxy_enable", web_proxy_enable);
		web_proxy_server = getStr("web_proxy_server", web_proxy_server);
		web_proxy_port = getInt("web_proxy_port", web_proxy_port);
		web_proxy_username = getStr("web_proxy_username", web_proxy_username);
		web_proxy_password = getStr("web_proxy_password", web_proxy_password);

		appId = getStr("appId", appId);
	}

	protected void setPreferences()
	{

		setStr("gui_dir", gui_dir);
		p.setProperty("gui_recent0", ""+gui_recent0);
		p.setProperty("gui_recent1", ""+gui_recent1);
		p.setProperty("gui_recent2", ""+gui_recent2);
		p.setProperty("gui_recent3", ""+gui_recent3);
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
		p.setProperty("gui_filter_table", "" + gui_filter_table);
		p.setProperty("gui_goto_nuc", "" + gui_goto_nuc);
		p.setProperty("gui_project_count", "" + gui_project_count);
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
		p.setProperty("gui_export_pars", "" + gui_export_pars);
		p.setProperty("gui_export_todisk", "" + gui_export_todisk);
		p.setProperty("gui_odialog_x", "" + gui_odialog_x);
		p.setProperty("gui_odialog_y", "" + gui_odialog_y);
		p.setProperty("gui_odialog_w", "" + gui_odialog_w);
		p.setProperty("gui_odialog_h", "" + gui_odialog_h);
		p.setProperty("gui_auto_min", "" + gui_auto_min);
		p.setProperty("gui_auto_discard", "" + gui_auto_discard);
		p.setProperty("gui_group_threshold", "" + gui_group_threshold);
		p.setProperty("gui_import_method", "" + gui_import_method);
		p.setProperty("gui_max_cpus", "" + gui_max_cpus);
		
		p.setProperty("gui_seq_tooltip", "" + gui_seq_tooltip);
		p.setProperty("gui_seq_font_size", "" + gui_seq_font_size);
		p.setProperty("gui_seq_font_bold", "" + gui_seq_font_bold);
		p.setProperty("gui_seq_show_text", "" + gui_seq_show_text);
		p.setProperty("gui_seq_antialias", ""+gui_seq_antialias);
		p.setProperty("gui_seq_show_colors", "" + gui_seq_show_colors);
		p.setProperty("gui_graph_smooth", "" + gui_graph_smooth);
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
		p.setProperty("gui_seq_highlight", setColor(gui_seq_highlight));
		p.setProperty("gui_graph_window", setColor(gui_graph_window));
		p.setProperty("gui_graph_threshold", setColor(gui_graph_threshold));
		p.setProperty("gui_graph_background", setColor(gui_graph_background));
		p.setProperty("gui_graph_color", setColor(gui_graph_color));
		p.setProperty("gui_show_horizontal_highlight", ""
				+ gui_show_horizontal_highlight);
		p.setProperty("gui_show_vertical_highlight", ""
				+ gui_show_vertical_highlight);

		p.setProperty("pdm2_window", "" + pdm2_window);
		p.setProperty("pdm2_step", "" + pdm2_step);

		p.setProperty("pdm_window", "" + pdm_window);
		p.setProperty("pdm_step", "" + pdm_step);
		p.setProperty("pdm_runs", "" + pdm_runs);
		p.setProperty("pdm_prune", "" + pdm_prune);
		p.setProperty("pdm_cutoff", "" + pdm_cutoff);
		p.setProperty("pdm_seed", "" + pdm_seed);
		p.setProperty("pdm_burn", "" + pdm_burn);
		p.setProperty("pdm_cycles", "" + pdm_cycles);
		setStr("pdm_burn_algorithm", pdm_burn_algorithm);
		setStr("pdm_main_algorithm", pdm_main_algorithm);
		p.setProperty("pdm_use_beta", "" + pdm_use_beta);
		p.setProperty("pdm_parameter_update_interval", ""
				+ pdm_parameter_update_interval);
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
		p.setProperty("dss_gap_threshold", ""+dss_gap_threshold);

		p.setProperty("lrt_window", "" + lrt_window);
		p.setProperty("lrt_step", "" + lrt_step);
		p.setProperty("lrt_runs", "" + lrt_runs);
		p.setProperty("lrt_method", "" + lrt_method);
		p.setProperty("lrt_gap_threshold", ""+lrt_gap_threshold);

		p.setProperty("qt_bootstrap", ""+qt_bootstrap);
		p.setProperty("qt_tstv", ""+qt_tstv);
		p.setProperty("qt_alpha", ""+qt_alpha);
		
		p.setProperty("mb_type", ""+mb_type);
		p.setProperty("mb_runs", ""+mb_runs);
		p.setProperty("mb_gens", ""+mb_gens);
		p.setProperty("mb_samplefreq", ""+mb_samplefreq);
		p.setProperty("mb_burnin", ""+mb_burnin);
		
		p.setProperty("rax_type", ""+rax_type);
		p.setProperty("rax_bootstrap", ""+rax_bootstrap);
		p.setProperty("rax_ratehet", ""+rax_ratehet);
		p.setProperty("rax_empfreq", ""+rax_empfreq);
		p.setProperty("rax_protmodel", ""+rax_protmodel);
			
		p.setProperty("phyml_bootstrap", ""+phyml_bootstrap);
		
		p.setProperty("ms_models", ms_models);
		p.setProperty("ms_gamma", ""+ms_gamma);
		p.setProperty("ms_inv", ""+ms_inv);
		p.setProperty("ms_samplesize", ms_samplesize);
		
		setStr("web_direct_url", web_direct_url);
		setStr("web_broker_url", web_broker_url);
		p.setProperty("web_use_rbroker", "" + web_use_rbroker);
		p.setProperty("web_check_secs", "" + web_check_secs);
		p.setProperty("web_check_startup", "" + web_check_startup);
		p.setProperty("web_proxy_enable", "" + web_proxy_enable);
		setStr("web_proxy_server", web_proxy_server);
		p.setProperty("web_proxy_port", "" + web_proxy_port);
		setStr("web_proxy_username", web_proxy_username);
		setStr("web_proxy_password", web_proxy_password);

		setStr("appId", appId);
	}

	public static void setDisplayDefaults()
	{
		gui_seq_tooltip = false;
		gui_seq_font_size = 12;
		gui_seq_font_bold = false;
		gui_seq_show_text = true;
		gui_seq_antialias = true;
		gui_seq_show_colors = true;
		gui_graph_smooth = true;
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
		gui_seq_highlight = new Color(0, 0, 230);
		gui_graph_window = new Color(0, 0, 255);
		gui_graph_threshold = new Color(0, 255, 64);
		gui_graph_background = new Color(255, 255, 255);
		gui_graph_color = new Color(0, 0, 130);
		gui_show_horizontal_highlight = true;
		gui_show_vertical_highlight = true;
	}

	public static void setPDM2Defaults()
	{
		pdm2_window = 500;
		pdm2_step = 10;
	}

	public static void setPDMDefaults()
	{
		pdm_window = 500;
		pdm_step = 10;
		pdm_runs = 100;
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
		pdm_initial_theta = "1";
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
		dss_window = 500;
		dss_step = 10;
		dss_runs = 100;
		dss_power = topali.cluster.jobs.dss.DSS.POWER_UNWEIGHTED;
		dss_method = topali.cluster.jobs.dss.DSS.METHOD_JC;
		dss_pass_count = topali.cluster.jobs.dss.DSS.ONE_PASS;
		dss_gap_threshold = 0.5;
	}

	public static void setLRTDefaults()
	{
		lrt_window = 500;
		lrt_step = 10;
		lrt_runs = 100;
		lrt_method = topali.cluster.jobs.lrt.LRT.METHOD_JC;
		lrt_gap_threshold = 0.5;
	}

	public static void setQTDefaults() {
		qt_bootstrap = 0;
		qt_tstv = 2.0;
		qt_alpha = 4.0;
		qt_bootstrap_default = 0;
		qt_tstv_default = 2.0;
		qt_alpha_default = 4.0;
	}
	
	public static void setMBDefaults() {
		mb_type = 0;
		mb_runs = 2;
		mb_gens = 100000;
		mb_samplefreq = 10;
		mb_burnin = 25;
		mb_default_dnamodel = "HKY";
		mb_default_proteinmodel = "WAG";
		mb_default_model_gamma = true;
		mb_default_model_inv = false;
		mb_runs_default = 2;
		mb_gens_default = 100000;
		mb_samplefreq_default = 10;
		mb_burnin_default = 25;
	}
	
	public static void setRaxDefaults() {
		rax_type = 0;
		rax_bootstrap = 100;
		rax_empfreq = false;
		rax_ratehet = "MIX";
		rax_protmodel = "WAG";
		rax_bootstrap_default = 100;
		rax_empfreq_default = false;
		rax_ratehet_default = "MIX";
		rax_protmodel_default = "WAG";
	}
	
	public static void setPhymlDefaults() {
		phyml_bootstrap = 0;
		phyml_bootstrap_default = 0;
		phyml_dnamodel_default = "HKY";
		phyml_proteinmodel_default = "WAG";
	}
	
	public static void setMSDefaults() {
		ms_models = ModelTestResult.TYPE_PHYML;
		ms_gamma = true;
		ms_inv = true;
		ms_samplesize = ModelTestResult.SAMPLE_SEQLENGTH;
	}
	
	public static void setWebDefaults()
	{
		web_direct_url = "http://www.topali.org/topali";
		web_broker_url = "http://www.topali.org/topali-broker";
		web_use_rbroker = true;
		web_check_secs = 30;
		web_check_startup = true;
		web_proxy_port = 8080;
		web_proxy_server = "";
		web_proxy_username = "";
		web_proxy_password = "";
	}
}