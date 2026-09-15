# Vec3 Migration Guide

## Goal

Replace all raw scalar (x, y, z) coordinate handling with vector and matrix operations.
Every place that manipulates 3D positions, offsets, or sizes as separate numbers is a candidate.

---

## What was already migrated

### Parameters / return types

```java
// before
void write(World world, int x, int y, int z, Block blk, int meta)
Vec3DInt mopToCoord(MovingObjectPosition mop) // returned int[]

// after
void write(World world, Vec3DInt coord, Block blk, int meta)
Vec3DInt mopToCoord(MovingObjectPosition mop)
```

```java
// before
boolean hasSolidNeighbor(World world, int x, int y, int z)
boolean inShape(BrushShape shape, int dx, int dy, int dz, int sx, int sy, int sz)
void forBrush(BrushState bs, int sx, int sy, int sz, VoxelAction action)

// after
boolean hasSolidNeighbor(World world, Vec3DInt coord)
boolean inShape(BrushShape shape, Vec3DInt offset, Vec3DInt brushSize)
void forBrush(BrushState bs, VoxelAction action)  // brushSize read from BrushState internally
```

### Lambda signatures

```java
// before
BrushUtil.forBrush(bs, (dx, dy, dz) -> { ... });

// after
BrushUtil.forBrush(bs, offset -> { ... });
// access components only when needed: offset.x(), offset.y(), offset.z()
```

### Local variable groups → Vec3DInt

```java
// before
int wx = ox + dx, wy = oy + dy, wz = oz + dz;
ChangeProposal.write(world, wx, wy, wz, blk, meta);

// after
Vec3DInt worldPos = origin.plus(offset);
ChangeProposal.write(world, worldPos, blk, meta);
```

### Arithmetic → vector ops

```java
// before
int dimX = brushRadius * 2 + 1;
int dimY = brushHeight * 2 + 1;
int dimZ = brushRadius * 2 + 1;

// after
Vec3DInt dims = brushSize.times(2).plus(1);
```

```java
// before
float ex = dx / (float) sx, ey = dy / (float) sy, ez = dz / (float) sz;
float dist = ex * ex + ey * ey + ez * ez;

// after
Vec3DFloat n = offset.toFloat().divide(brushSize.toFloat());
float dist = n.dot(n);
```

### Cached dim* locals → inline accessor calls

```java
// before
int dimX = dims.x(), dimY = dims.y(), dimZ = dims.z();
int snStX = dimY * dimZ;
// ... dimX/dimY/dimZ used throughout

// after
int snStX = dims.y() * dims.z();
// ... dims.x() / dims.y() / dims.z() inline
```

Dead extractions (`dimX` unused in index arithmetic but declared) were deleted outright.

### Scalar triple + vector construction → single vector pipeline

```java
// before
int wx = wv.x(), wy = wv.y(), wz = wv.z();
float dx = wx + 0.5f - cm.x();
float dy = wy + 0.5f - cm.y();
float dz = wz + 0.5f - cm.z();
Vec3DFloat rv = R.mul(Vec3DFloat.from(dx, dy, dz));
float rx = rv.x(), ry = rv.y(), rz = rv.z();
int nx = (int) Math.floor(cm.x() + delta.x() + rx);
// ...

// after
Vec3DFloat rv = R.mul(wv.toFloat().plus(0.5f).minus(cm));
Vec3DFloat nPos = cm.plus(delta).plus(rv);
int nx = (int) Math.floor(nPos.x());
// ...
```

### Intermediate min/max scalar sextuples → Vec3DInt pair

```java
// before
int minX = sel.minX(), maxX = sel.maxX();
int minY = sel.minY(), maxY = sel.maxY();
int minZ = sel.minZ(), maxZ = sel.maxZ();
Vec3DInt origin = Vec3DInt.from(minX, minY, minZ).minus(1);
Vec3DInt end    = Vec3DInt.from(maxX, maxY, maxZ).plus(1);
// later: Vec3DInt.forEachInclusive(Vec3DInt.from(minX, minY, minZ), ...)

// after
Vec3DInt origin = Vec3DInt.from(sel.minX(), sel.minY(), sel.minZ()).minus(1);
Vec3DInt end    = Vec3DInt.from(sel.maxX(), sel.maxY(), sel.maxZ()).plus(1);
// later: Vec3DInt.forEachInclusive(origin.plus(1), end.minus(1), ...)
```

### Flat array indexing

```java
// before
int idx = (dx + sx + margin) * snStX + (dy + sy + margin) * snStY + (dz + sx + margin);

// after
int idx = offset.plus(sx + margin, sy + margin, sz + margin).toIndex(snStX, snStY);
```

### Neighbour iteration

```java
// before
int[][] FACE_DIRS = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
for (int[] d : FACE_DIRS) {
    Block b = world.getBlock(x + d[0], y + d[1], z + d[2]);
}

// after
for (Vec3DInt d : BlockUtils.NEIGHBOUR_OFFSETS) {
    Block b = WorldUtils.getBlock(world, coord.plus(d));
}
```

---

## How to find instances that still need migrating

### 1. Parameter lists with (int x, int y, int z) or (int sx, int sy, int sz)

```
grep -rn "int x, int y, int z\|int sx, int sy, int sz\|int ox, int oy, int oz\|int wx, int wy, int wz"
```

Any method taking three consecutive int parameters named after axes is a candidate to accept `Vec3DInt` instead.

### 2. Triple local variable groups

```
grep -rn "int wx =\|int wy =\|int wz =\|int ox =\|int oy =\|int oz ="
```

Also look for patterns like `int dimX =`, `int dimY =`, `int dimZ =` — size triples become `Vec3DInt`.

### 3. Fields declared as separate x/y/z or stored in int[]

```
grep -rn "int\[\] pos\|int\[\] coord\|int\[\] origin\|public int x;\|public int y;\|public int z;"
```

Class fields storing a position as three ints (or an `int[]`) should become `Vec3DInt`.

Also catch axis-suffixed field names (e.g. `gradientPos1X`, `anchorPosY`) that the bare-name pattern above misses:

```
grep -rn "public int [a-zA-Z0-9]*[XYZ] \?="
```

Filter false positives: a single suffixed field (e.g. `offsetY` with no `offsetX`/`offsetZ`, or `torusRingRadiusZ` paired with a non-axis sibling) is not a spatial triple — check that all three axis suffixes exist together before migrating.

### 4. Calculations on scalar x/y/z that could be vector ops

#### Squared distance / length
```
grep -rn "\* dx\|\* dy\|\* dz\|\* kx\|\* ky\|\* kz\|\* ex\|\* ey\|\* ez" | grep "+"
```
`a*a + b*b + c*c` → `v.dot(v)` (= `v.lengthSq()` for ints)
`Math.sqrt(a*a + b*b + c*c)` → `v.length()`

#### Component-wise divide / multiply
```
grep -rn "/ rx\|/ ry\|/ rz\|/ sx\|/ sy\|/ sz"
```
Three parallel divides `ex = dx/rx, ey = dy/ry, ez = dz/rz` → `offset.toFloat().divide(size.toFloat())`

#### Component-wise min/max
```
grep -rn "Math\.min.*Math\.min\|Math\.max.*Math\.max"
```
`Math.min(ax, Math.min(ay, az))` → `v.min()` or `v.min(other)`

#### Negation
```
grep -rn "-dx\|-dy\|-dz\|-sx\|-sy\|-sz"
```
`Vec3DInt.from(-dx, -dy, -dz)` → `offset.negate()`
`Vec3DInt.from(-kR, -kR, -kR)` → `bound.negate()`

#### Product of dimensions
```
grep -rn "dimX \* dimY\|dimY \* dimZ\|sx \* sy\|sy \* sz"
```
`dimX * dimY * dimZ` → `dims.product()`

### 5. Triple nested loops over a volume

The single-line grep below misses multi-line loop headers — use the z-axis outermost loop as the signal instead:

```
grep -rn "for (int dz\|for (int kz\|for (int oz\|for (int iz\|for (int nz"
```

Check 2 lines of context (`-B2`) to confirm dx/dy loops are nested above. Exclude `NoiseSampler.java` (performance-sensitive noise internals) and `PathMath.java` (Bresenham/addSphere algorithm math).

Replace confirmed triple loops with `Vec3DInt.forEachInclusive(min, max, (x, y, z) -> { ... })` unless the loop body uses flat-array index arithmetic that depends on the scalar loop variable directly (hot-path brush kernels in `BrushUtil`, `SmoothBrush`, `DistortBrush` — leave as scalars there but add a comment).

### 6. Inline world access with raw coords

```
grep -rn "world\.getBlock(.*,.*,.*)\|world\.setBlock(.*,.*,.*,.*)"
```
Any call using bare `x, y, z` ints where a `Vec3DInt` is available or constructible should go through `WorldUtils`.

### 7. Flat-array index arithmetic

```
grep -rn "\* snStX\|\* snStY\|\* strideX\|\* strideY\|\* dimZ\|\* dims\.z()"
```
When the three components of the index already exist as a `Vec3DInt`, use `v.toIndex(strideX, strideY)`.

### Axis-suffixed field triples → Vec3DInt fields

```java
// before (GradientToolState)
public int gradientPos1X = 0;
public int gradientPos1Y = 0;
public int gradientPos1Z = 0;
public int gradientPos2X = 0;
public int gradientPos2Y = 0;
public int gradientPos2Z = 0;

// after
public Vec3DInt gradientPos1 = Vec3DInt.ZERO;
public Vec3DInt gradientPos2 = Vec3DInt.ZERO;
```

Callsites updated: `GradientBrushInput` (assign from `mop.block{X,Y,Z}`), `GradientBrush` (convert to `Vec3DDouble` via `.toDouble()`), `SelectionRenderer` (build camera-relative `Vec3DDouble` via `.toDouble().plus(0.5).minus(camPos)`).

### Scalar triple → Vec3DInt in captured lambda variable

```java
// before (SelectionState.captureFromWorld)
int ox = minX(), oy = minY(), oz = minZ();
clipDim.forEach((x, y, z) -> {
    if (contains(ox + x, oy + y, oz + z)) {
        Block block = world.getBlock(ox + x, oy + y, oz + z);
        int meta = world.getBlockMetadata(ox + x, oy + y, oz + z);

// after
Vec3DInt clipOrigin = Vec3DInt.from(minX(), minY(), minZ());
clipDim.forEach((x, y, z) -> {
    Vec3DInt wc = clipOrigin.plus(x, y, z);
    if (contains(wc.x(), wc.y(), wc.z())) {
        Block block = WorldUtils.getBlock(world, wc);
        int meta = WorldUtils.getBlockMetadata(world, wc);
```

### BFS flood-fill with int[] → Vec3DInt queue + NEIGHBOUR_OFFSETS

```java
// before (SelectionOps.fillEnclosedOps)
int ox = origin.x(), oy = origin.y(), oz = origin.z();
int ex = end.x(), ey = end.y(), ez = end.z();
Queue<int[]> queue = new LinkedList<>();
int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
while (!queue.isEmpty()) {
    int[] cur = queue.poll();
    for (int[] d : dirs) {
        int nx = cur[0]+d[0], ny = cur[1]+d[1], nz = cur[2]+d[2];
        if (nx < ox || nx > ex || ...) continue;

// after
Queue<Vec3DInt> queue = new LinkedList<>();
while (!queue.isEmpty()) {
    Vec3DInt cur = queue.poll();
    for (Vec3DInt d : BlockUtils.NEIGHBOUR_OFFSETS) {
        Vec3DInt next = cur.plus(d);
        if (!next.inBounds(origin, end)) continue;
```

Also migrated `idx(int x, int y, int z, ...)` helper → `idx(Vec3DInt v, ...)`.

### unpackX/Y/Z triple → unpackKey

```java
// before (GuiDimensiumOverlay)
int wx = ChangeProposal.unpackX(key),
        wy = ChangeProposal.unpackY(key),
        wz = ChangeProposal.unpackZ(key);
if (world.getBlock(wx, wy, wz) != Blocks.air) continue;
ops.add(new int[] { ChangeProposal.unpackX(key), ChangeProposal.unpackY(key), ChangeProposal.unpackZ(key), bm[0], bm[1] });

// after
Vec3DInt wc = ChangeProposal.unpackKey(key);
if (WorldUtils.getBlock(world, wc) != Blocks.air) continue;
ops.add(new int[] { wc.x(), wc.y(), wc.z(), bm[0], bm[1] });
```

---

## Decision heuristic

When you find raw scalar coords, ask in order:

1. **Does a Vec3DInt already exist here?** Use it directly — don't extract `.x()/.y()/.z()` unless forced.
2. **Are three scalars constructed together and used together?** Merge them into a Vec3DInt immediately at construction.
3. **Is the calculation component-wise symmetric** (same op applied to x, y, z independently)? Use a vector op (`plus`, `times`, `divide`, `negate`, etc.).
4. **Is the calculation a reduction** (sum, product, min, max, dot, length)? Use the matching Vec3D method.
5. **Is it a hot inner loop** where creating a Vec3DInt per iteration costs more than the inline math? Leave as scalars but add a comment noting why.

---

## Remaining known instances

None — all spatial 3D coordinate groups have been migrated.

### Triple loop → `forEachInclusive` in `ShapeBrush`

```java
// before
int x = mop.blockX - (w - 1) / 2;
int y = mop.blockY - (h - 1) / 2;
int z = mop.blockZ - (d - 1) / 2;
for (int dx = 0; dx < w; dx++)
    for (int dy = 0; dy < h; dy++)
        for (int dz = 0; dz < d; dz++) {
            if (s.shapeKeepExisting && world.getBlock(x + dx, y + dy, z + dz) != Blocks.air) continue;
            ChangeProposal.write(world, Vec3DInt.from(x + dx, y + dy, z + dz), blk, meta);
        }

// after
Vec3DInt origin = Vec3DInt.from(mop.blockX - (fw-1)/2, mop.blockY - (fh-1)/2, mop.blockZ - (fd-1)/2);
Vec3DInt.forEachInclusive(Vec3DInt.ZERO, Vec3DInt.from(fw-1, fh-1, fd-1), (dx, dy, dz) -> {
    Vec3DInt pos = origin.plus(dx, dy, dz);
    if (s.shapeKeepExisting && WorldUtils.getBlock(world, pos) != Blocks.air) return;
    ChangeProposal.write(world, pos, blk, meta);
});
```

Note: `w`, `h`, `d` are reassigned above before the lambda, so final copies `fw`, `fh`, `fd` are needed.

### `int[][]` neighbour arrays → `Vec3DInt[]` constants

```java
// before (SelectionState, AutoshadeWindow, SelectionOps)
int[][] dirs6 = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
int[][] dirs26 = buildDirs26(); // local triple-nested loop
int[][] faces = {{1,0,0},...};
for (int[] d : dirs) {
    Vec3DInt nb = Vec3DInt.from(cur.x() + d[0], cur.y() + d[1], cur.z() + d[2]);

// after
Vec3DInt[] dirs = corners ? BlockUtils.NEIGHBOURS_26_OFFSETS : BlockUtils.NEIGHBOUR_OFFSETS;
for (Vec3DInt d : dirs) {
    Vec3DInt nb = cur.plus(d);
```

`BlockUtils` gained `NEIGHBOURS_26_OFFSETS` (26-neighbour constant, computed via `Vec3DInt.forEachInclusive`).

### Brush size triple → Vec3DInt

```java
// before (BrushPreviewRenderer)
void getBrushWireframe(BrushShape shape, int sx, int sy, int sz)
private int cachedBrushSize, cachedBrushSizeY, cachedBrushSizeZ;

// after
void getBrushWireframe(BrushShape shape, Vec3DInt brushSize)
private Vec3DInt cachedBrushSize;
```

`inBrushShape(shape, dx, dy, dz, sx, sy, sz)` wrapper deleted — callers use `BrushUtil.inShape` directly.
`buildBrushSet` and `collectSolidAbsolute` loop bodies migrated to `Vec3DInt.forEachInclusive`.

### Terrain placement position → Vec3DInt

```java
// before (ElevationBrush)
placeTerrainBlock(world, int wx, int wy, int wz, int sourceY, boolean continuous)

// after
placeTerrainBlock(world, Vec3DInt pos, int sourceY, boolean continuous)
```

## Loops with early-exit — cannot use `forEachInclusive`

| Location | Reason |
|---|---|
| `ShapeMath.iterateRotatedShape` | `ShapeVoxelConsumer.accept` returns `boolean` to abort — maps to inverted `anyInclusive` semantics, left as triple loop |
| `SelectionOps.fillNearestOps` | Shell-radius BFS with `break outer` across radius and offset loops — body already uses `cv.plus(dx,dy,dz)`, no scalar coord leakage |

---

## Intentionally excluded

| File                 | Reason                                                                     |
|----------------------|----------------------------------------------------------------------------|
| `ModellingMath.java` | Uses `double[]` arrays throughout; different migration track               |
| `NoiseSampler.java`  | Low-level noise internals; performance-sensitive, not spatial coords       |
| `PathMath.java`      | Bresenham line rasterizer; `err1 += 2*dy` is algorithm math, not coord ops |
