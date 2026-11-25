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
  /** En dessous, le texte devient illisible au projecteur : on laisse alors défiler. */
  static final double MIN_ZOOM = 0.6;
  /**
   * Les animations en boucle (carte énergie : nuages, flux, pluie…) font redessiner la page
   * ~40 fois/s et chauffer le projecteur. On les met en pause et on les avance par pas de
   * CALM_MS (2 pas/s) : le mouvement reste visible, le rendu tombe à ~2 images/s.
   */
  static final int CALM_MS = 500;

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
      "timer=setInterval(function(){tries++;if(hideHeader()){clearInterval(timer);log('kiosk:ok');refit();}else if(tries>=40){clearInterval(timer);}},500);}",
      "window.addEventListener('location-changed',retry);retry();",
      // (c) couleurs du launcher et zoom
      "function theme(){var r=document.documentElement;r.style.setProperty('--primary-background-color','#0a0e1d');",
      "r.style.setProperty('--lovelace-background','#0a0e1d');r.style.setProperty('--card-background-color','#151c35');",
      "r.style.setProperty('--ha-card-background','#151c35');}",
      // (c') tout tient à l'écran : zoom réduit jusqu'à ce que la vue ne déborde plus (borné à ZMIN)
      "var ZMAX=" + zoom + ",ZMIN=" + MIN_ZOOM + ",z=ZMAX,fitT=[];",
      "try{var sb=document.createElement('style');sb.textContent='::-webkit-scrollbar{display:none;width:0;height:0}';document.head.appendChild(sb);}catch(e){}",
      "function scroller(){var c=[document.scrollingElement],hr=deep(document,'hui-root',0);",
      "if(hr&&hr.shadowRoot){var v=hr.shadowRoot.getElementById('view');if(v)c.push(v);",
      "if(!hr.shadowRoot.getElementById('hy-sb')){var s2=document.createElement('style');s2.id='hy-sb';s2.textContent='::-webkit-scrollbar{display:none;width:0;height:0}';hr.shadowRoot.appendChild(s2);}}",
      "var best=null,ov=0;for(var i=0;i<c.length;i++){var e=c[i];if(e&&e.clientHeight>0){var q=e.scrollHeight/e.clientHeight;if(q>ov){ov=q;best=e;}}}return best;}",
      "function apply(){document.documentElement.style.zoom=String(z);}",
      "function fit(n){var e=scroller();if(!e)return;var q=e.scrollHeight/e.clientHeight;",
      "if(q>1.01&&z>ZMIN&&n<6){z=Math.max(ZMIN,Math.floor(z/q*0.99*100)/100);apply();fitT.push(setTimeout(function(){fit(n+1);},400));}",
      "else log('fit z='+z+' q='+q.toFixed(2));}",
      "function refit(){for(var i=0;i<fitT.length;i++)clearTimeout(fitT[i]);fitT=[];z=ZMAX;apply();",
      "fitT.push(setTimeout(function(){fit(0);},700));fitT.push(setTimeout(function(){fit(0);},2500));fitT.push(setTimeout(function(){fit(0);},6000));}",
      "window.addEventListener('location-changed',refit);window.addEventListener('resize',refit);refit();",
      // (c'') animations infinies ralenties : pause + avance par pas (les transitions de l'UI HA ne sont pas touchées)
      "var CALM=" + CALM_MS + ";var calmN=-1,calmK=0,R=[];",
      // document.getAnimations() ne voit pas l'intérieur des shadow roots : on les collecte
      "function roots(r,d,out){if(!r||d>14||!r.querySelectorAll)return;var all=r.querySelectorAll('*');",
      "for(var i=0;i<all.length;i++){var s=all[i].shadowRoot;if(s){out.push(s);roots(s,d+1,out);}}}",
      "function calm(){if(calmK%10===0){R=[document];roots(document,0,R);}calmK++;var n=0;",
      "for(var k=0;k<R.length;k++){var l;try{l=R[k].getAnimations();}catch(e){continue;}",
      "for(var i=0;i<l.length;i++){var a=l[i];try{var t=a.effect&&a.effect.getTiming?a.effect.getTiming():null;",
      "if(!t||!(t.iterations===Infinity))continue;if(a.playState!=='paused')a.pause();a.currentTime=(a.currentTime||0)+CALM;n++;}catch(e){}}}",
      "if(n!==calmN){calmN=n;log('calm n='+n+' racines='+R.length);}}",
      "setInterval(calm,CALM);",
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
