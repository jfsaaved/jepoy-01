#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;

uniform float u_time;
uniform vec2  u_spritePos;
uniform vec2  u_spriteSize;
uniform vec2  u_texel;

uniform float u_darkStart, u_darkEnd, u_ambient;
uniform vec2  u_firePos;
uniform float u_radius, u_flickerAmp, u_flickerHz;
uniform vec3  u_tintColor;
uniform float u_tintStrength, u_tintHeight, u_tintPulseAmp, u_tintPulseHz;
uniform float u_waveAmpPx, u_waveFreqX, u_waveFreqY, u_waveSpeed, u_waveFalloff, u_waveTurbulence;

/* NEW: control how much of the effect applies to rocks vs bright flame */
uniform float u_flameCutoff;   // ~0.65 : pixels brighter than this are “flame”
uniform float u_flameFeather;  // ~0.15 : softness of separation
uniform float u_effectRock;    // 1.0   : full effect on rock/dark pixels
uniform float u_effectFlame;   // 0.2   : light touch on bright flame

float hash(float n){ return fract(sin(n)*43758.5453123); }
float noise1(float x){
  float i=floor(x), f=fract(x);
  float u=f*f*(3.0-2.0*f);
  return mix(hash(i), hash(i+1.0), u);
}

void main() {
  // sprite-local coords [0..1], bottom-up
  vec2 p = (gl_FragCoord.xy - u_spritePos) / u_spriteSize;
  p = clamp(p, 0.0, 1.0);
  float yUp = p.y;

  // heat shimmer -> UV offset in pixels, convert to UV
  float fall  = 1.0 - smoothstep(0.0, max(u_waveFalloff, 0.0001), yUp);
  float phase = u_time * u_waveSpeed;
  float sx = sin(p.y * u_waveFreqY - phase);
  float sy = sin(p.x * u_waveFreqX + phase * 0.7);
  float n  = noise1(phase * 1.3 + p.y * 5.0);
  float ampPx = u_waveAmpPx * fall;

  vec2 uv = v_texCoords + vec2(
      ampPx * (0.70*sx + 0.30*sy + u_waveTurbulence * (n - 0.5)) * u_texel.x,
      ampPx * 0.35 * (sy * 0.5 + (n - 0.5) * 0.6) * u_texel.y
  );

  vec4 base = texture2D(u_texture, uv) * v_color;
  if (base.a <= 0.0) discard;

  // base gradient (top darker)
  float t = smoothstep(u_darkStart, u_darkEnd, yUp);
  float baseFactor = mix(1.0, u_ambient, t);

  // fire pocket + flicker
  float nF = noise1(u_time * (u_flickerHz * 0.77));
  float s1 = 0.5 + 0.5 * sin(u_time * (u_flickerHz * 1.33));
  float s2 = 0.5 + 0.5 * sin(u_time * (u_flickerHz * 2.11) + 1.2);
  float bands  = 0.5 + 0.5 * sin((p.y * 11.0 - u_time * (u_flickerHz * 0.9))
                               + 0.7 * sin(u_time * 3.1 + p.x * 6.0));

  float d      = distance(p, u_firePos);
  float jitter = 0.90 + 0.25 * (0.6*nF + 0.4*s1);
  float rEff   = u_radius * jitter * mix(0.90, 1.10, bands);
  float radial = clamp(1.0 - smoothstep(0.0, rEff, d), 0.0, 1.0);

  float liftFlick = 1.0 + u_flickerAmp * ((nF - 0.5) * 1.4 + (s1 - 0.5) * 0.8);
  float lift      = radial * (1.0 - baseFactor) * max(liftFlick, 0.0);
  float factor    = clamp(baseFactor + lift, u_ambient, 1.0);

  // yellow tint near feet
  float vertMask  = 1.0 - smoothstep(0.0, u_tintHeight, yUp);
  float tintMask  = radial * vertMask * (0.75 + 0.25 * bands);
  float tintPulse = 1.0 + u_tintPulseAmp * (s2 - 0.5);
  vec3  tintAdd   = u_tintColor * (u_tintStrength * tintMask * tintPulse) * base.a;

  // --- NEW: apply effect more on rocks, less on bright flame ---
  float heat = max(max(base.r, base.g), base.b);  // brightness proxy
  float flameMask = smoothstep(u_flameCutoff - u_flameFeather,
                               u_flameCutoff + u_flameFeather, heat); // 0=rock,1=flame
  float effectMask = mix(u_effectRock, u_effectFlame, flameMask);

  vec3 shaded = base.rgb * factor + tintAdd;
  vec3 outRgb = mix(base.rgb, shaded, clamp(effectMask, 0.0, 1.0));

  gl_FragColor = vec4(clamp(outRgb, 0.0, 1.0), base.a);
}
