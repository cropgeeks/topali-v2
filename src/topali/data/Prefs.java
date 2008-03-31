// (C) 2003-2007 Biomathematics & Statistics Scotland
//
// This package may be distributed under the
// terms of the GNU General Public License (GPL)

package topali.data;

import java.awt.Color;
import java.util.Random;

import scri.commons.gui.XMLPreferences;
import topali.mod.Filters;

public class Prefs extends XMLPreferences
{
    public static String locale;
    
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
	public static int gui_seq_font_size;
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
	public static boolean dss_varwindow;
	
	// LRT analysis run settings
	public static int lrt_window, lrt_step, lrt_runs, lrt_method;
	public static double lrt_gap_threshold;
	public static boolean lrt_varwindow;

	// Quick tree settings
	public static int qt_bootstrap;
	public static double qt_tstv;
	public static double qt_alpha;
	public static boolean qt_estimate;
	public static int qt_bootstrap_default;
	public static double qt_tstv_default;
	public static double qt_alpha_default;
	public static boolean qt_estimate_default;
	
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

	public Prefs()
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
	}

	public static void setDisplayDefaults()
	{
	    	locale = "default";
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
		dss_power = topali.cluster.jobs.dss.analysis.DSS.POWER_UNWEIGHTED;
		dss_method = topali.cluster.jobs.dss.analysis.DSS.METHOD_JC;
		dss_pass_count = topali.cluster.jobs.dss.analysis.DSS.ONE_PASS;
		dss_gap_threshold = 0.5;
		dss_varwindow = true;
	}

	public static void setLRTDefaults()
	{
		lrt_window = 500;
		lrt_step = 10;
		lrt_runs = 100;
		lrt_method = topali.cluster.jobs.lrt.analysis.LRT.METHOD_JC;
		lrt_gap_threshold = 0.5;
		lrt_varwindow = true;
	}

	public static void setQTDefaults() {
		qt_bootstrap = 0;
		qt_tstv = 2.0;
		qt_alpha = 4.0;
		qt_estimate = true;
		qt_bootstrap_default = 0;
		qt_tstv_default = 2.0;
		qt_alpha_default = 4.0;
		qt_estimate_default = true;
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