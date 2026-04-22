# Mathinix 🧮⚡

Mathinix is a JavaFX math app that explores the full power of *ParserNG* math library; to plot 2D and 3D graphs.
It gives the user a simple terminal/commandline style interface to do things that are not convenient to do on a simple calculator interface.

A command like:
`plot(y(x)=sin(x))`, immediately spins up a 2D graph of sin(x) and a command like:
`plot3d(sqrt(144-(x^2+y^2)))` will bring up a 3D graph of the specified expression.
You may also do:

`plot3d(sin(sqrt(x^2+y^2)), 100)`. The `100` specifies the resolution at which the plot should be shown.

You may rotate functions with:
rot()
