package com.hy.home;

/**
 * Script injecté dans la page Home Assistant après chaque chargement.
 * Il masque la barre latérale et l'en-tête de vue, passe l'interface en sombre,
 * pose les couleurs du launcher, et dialogue avec l'app par l'objet natif {@code bridge}.
 * Il n'agit sur l'en-tête que si {@code hui-root} existe : la page de connexion n'est pas touchée.
 */
public final class KioskScript {
  static final String WEATHER = "weather.ideaux4";
  static final String TEMPERATURE = "sensor.ideaux4_temperature";

  private KioskScript() {}

  public static String build(String bridge, double zoom) {
    String b = bridge;
    String[] js = {
      "(function(){if(window.__hyKiosk)return;window.__hyKiosk=1;",
      "function log(m){try{" + b + ".log(String(m));}catch(e){}}",
      // (a) barre latérale masquée + thème sombre, mémorisés par HA dans le localStorage
      "try{var ch=false;",
      "if(localStorage.getItem('dockedSidebar')!=='\"always_hidden\"'){localStorage.setItem('dockedSidebar','\"always_hidden\"');ch=true;}",
      "var dk=false;try{dk=JSON.parse(localStorage.getItem('selectedTheme')||'{}').dark===true;}catch(e){}",
      "if(!dk){localStorage.setItem('selectedTheme',JSON.stringify({\"theme\":\"default\",\"dark\":true}));ch=true;}",
      "if(ch&&!sessionStorage.getItem('hyReload')){sessionStorage.setItem('hyReload','1');location.reload();return;}",
      "}catch(e){log('storage '+e);}",
      // recherche à travers les shadow roots
      "function deep(root,sel,d){if(!root||d>12)return null;var f=root.querySelector?root.querySelector(sel):null;if(f)return f;",
      "var all=root.querySelectorAll?root.querySelectorAll('*'):[];for(var i=0;i<all.length;i++){var s=all[i].shadowRoot;",
      "if(s){var r=deep(s,sel,d+1);if(r)return r;}}return null;}",
      // (b) en-tête de vue masqué
      "function hideHeader(){var hr=deep(document,'hui-root',0);if(!hr||!hr.shadowRoot)return false;",
      "if(!hr.shadowRoot.getElementById('hy-kiosk')){var st=document.createElement('style');st.id='hy-kiosk';",
      "st.textContent='.header{display:none!important}#view,hui-view-container{padding-top:0!important;margin-top:0!important;min-height:100vh!important}';",
      "hr.shadowRoot.appendChild(st);}hr.style.setProperty('--header-height','0px');return true;}",
      "var tries=0,timer=null;function retry(){tries=0;if(timer)clearInterval(timer);",
      "timer=setInterval(function(){tries++;if(hideHeader()){clearInterval(timer);log('kiosk:ok');}else if(tries>=40){clearInterval(timer);}},500);}",
      "window.addEventListener('location-changed',retry);retry();",
      // (c) couleurs du launcher et zoom
      "function theme(){var r=document.documentElement;r.style.setProperty('--primary-background-color','#0a0e1d');",
      "r.style.setProperty('--lovelace-background','#0a0e1d');r.style.setProperty('--card-background-color','#151c35');",
      "r.style.setProperty('--ha-card-background','#151c35');r.style.zoom='" + zoom + "';}",
      // (d) API appelée par l'app
      "function hass(){var ha=document.querySelector('home-assistant');return ha&&ha.hass?ha.hass:null;}",
      "window.__hy={",
      "nav:function(p){history.pushState(null,'',p);window.dispatchEvent(new CustomEvent('location-changed',{detail:{replace:false}}));},",
      "press:function(id){var h=hass();if(!h){try{" + b + ".onPress(id,false);}catch(e){}return;}",
      "h.callService('input_button','press',{entity_id:id}).then(function(){try{" + b + ".onPress(id,true);}catch(e){}},",
      "function(){try{" + b + ".onPress(id,false);}catch(e){}});}",
      "};",
      // (e) état envoyé à l'app toutes les 30 s : « connecté|condition|température »
      "function status(){var h=hass();var c=h&&h.connected?1:0;var w=h&&h.states?h.states['" + WEATHER + "']:null;",
      "var t=h&&h.states?h.states['" + TEMPERATURE + "']:null;",
      "try{" + b + ".onStatus(c+'|'+(w?w.state:'')+'|'+(t?t.state:''));}catch(e){}theme();}",
      "theme();setTimeout(status,3000);setInterval(status,30000);",
      "})();"
    };
    StringBuilder sb = new StringBuilder();
    for (String line : js) sb.append(line).append('\n');
    return sb.toString();
  }
}
