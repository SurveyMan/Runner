package edu.umass.cs.runner.system.backend;

/**
 * Created by etosch on 3/20/15.
 * Scheduling policies.
 *
 * <ul>
 *     <li><b>FRONT</b>: Posts all tasks at once.</li>
 *     <li><b>ADAPT</b>: Posts tasks adaptively.</li>
 *     <li><b>N_LY</b>: Posts tasks according to a time increment (e.g., hourly).</li>
 * </ul>
 */
public enum Schedule {
   FRONT, ADAPT, N_LY;
}
