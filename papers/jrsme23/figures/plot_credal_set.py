# !pip install matplotlib
#!pip install --upgrade matplotlib numpy

from mpl_toolkits.mplot3d import Axes3D
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
import matplotlib.pyplot as plt

fig = plt.figure()
ax = Axes3D(fig, elev=30, azim=-60, roll=0)

x = [0, 1, 1, 0]
y = [0, 0, 1, 1]
z = [0, 1, 0, 1]
verts = [list(zip(x, y, z))]
print(verts)
ax.add_collection3d(Poly3DCollection(verts))
plt.show()