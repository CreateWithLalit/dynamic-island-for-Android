# Asset Generation Prompts for Immersive Weather

Use these prompts in Midjourney, DALL-E 3, or Leonardo.ai to generate the photorealistic assets required for the hybrid weather engine.

## 1. Seamless Sky Panoramas
**Goal**: 360-degree or wide panoramic sky backgrounds with no ground.

- **Dawn**: `Seamless panoramic sky texture, deep purple to peach sunrise, soft volumetric clouds, warm horizon glow, no ground, no horizon line, photorealistic, 8k, cinematic lighting, wrap-around seamless edges --ar 2:1`
- **Morning**: `Seamless panoramic sky texture, soft blue morning sky, wispy cirrus clouds, gentle directional sunlight, no ground, photorealistic, 8k --ar 2:1`
- **Noon**: `Seamless panoramic sky texture, vibrant deep blue sky, fluffy white cumulus clouds, bright high sun, no ground, photorealistic, 8k --ar 2:1`
- **Golden Hour**: `Seamless panoramic sky texture, amber orange sky, long shadows through clouds, warm atmospheric haze, no ground, photorealistic, 8k --ar 2:1`
- **Dusk**: `Seamless panoramic sky texture, burnt orange to deep red dusk, darkening clouds, fading light, no ground, photorealistic, 8k --ar 2:1`
- **Night**: `Seamless panoramic sky texture, deep navy starry night sky, subtle milky way, dark silhouetted clouds, no ground, photorealistic, 8k --ar 2:1`

## 2. Celestial Bodies (Sprites)
**Goal**: High-resolution icons with transparency (use "on black background" if you plan to use Screen/Additive blending, or "on transparent background" if the tool supports it).

- **Sun**: `Glowing sun orb, bright yellow core with white hot center, soft solar flares, realistic corona, black background, high resolution --v 6.0`
- **Moon**: `Hyper-realistic full moon, visible craters and lunar maria, 8k texture, isolated on black background --v 6.0`

## 3. Cloud Sprites
- **Puffy Clouds**: `Isolated fluffy white cloud, volumetric lighting, soft edges, high resolution, transparent background --v 6.0`
- **Storm Clouds**: `Isolated dark heavy storm cloud, internal purple and grey tones, dramatic lighting, high resolution, transparent background --v 6.0`

## 4. Overlays
- **Lens Flare**: `Photorealistic lens flare artifacts, hexagonal bokeh, light leaks, isolated on black background`
- **Vignette**: `Radial gradient vignette, soft black edges to transparent center, high resolution`

## Asset Integration Guide
1. **Convert to WebP**: Use squoosh.app or similar to convert all assets to `.webp` for the best quality-to-size ratio.
2. **Naming**: Name them exactly as they appear in `ImmersiveWeatherSceneHybrid.kt` (e.g., `sky_dawn.webp`, `sun_body.webp`).
3. **Placement**: Place them in `app/src/main/res/drawable/`.
