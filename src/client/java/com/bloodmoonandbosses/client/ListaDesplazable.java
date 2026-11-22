package com.bloodmoonandbosses.client;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.Mth;

/**
 * En 1.21.1 no existe ScrollableLayout (el de 26.x y 1.21.11): un contenedor con la rueda del mouse y una barra que se
 * puede arrastrar, con los mismos métodos que usa la pantalla. Los widgets que quedan fuera de la vista no se dibujan
 * (scissor) ni reciben clics.
 */
final class ListaDesplazable extends AbstractContainerWidget {
	private static final int ANCHO_BARRA = 6;
	private static final int SEPARACION_BARRA = 6;
	private static final int ALTO_MINIMO_BARRA = 16;
	private static final double PASO_DE_LA_RUEDA = 20;

	private final Layout contenido;
	private final List<AbstractWidget> hijos = new ArrayList<>();
	private int altoMaximo;
	private double desplazamiento;
	private boolean arrastrandoBarra;

	ListaDesplazable(Layout contenido, int altoMaximo) {
		super(0, 0, 0, 0, CommonComponents.EMPTY);
		this.contenido = contenido;
		this.altoMaximo = altoMaximo;
		contenido.visitWidgets(hijos::add);
		arrangeElements();
	}

	void setMaxHeight(int alto) {
		altoMaximo = alto;
		arrangeElements();
	}

	void arrangeElements() {
		contenido.arrangeElements();
		width = contenido.getWidth() + SEPARACION_BARRA + ANCHO_BARRA;
		height = Math.min(contenido.getHeight(), altoMaximo);
		desplazar(0);
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		ubicar();
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		ubicar();
	}

	private int maximo() {
		return Math.max(0, contenido.getHeight() - height);
	}

	private void desplazar(double cuanto) {
		desplazamiento = Mth.clamp(desplazamiento + cuanto, 0, maximo());
		ubicar();
	}

	private void ubicar() {
		contenido.setPosition(getX(), getY() - (int) desplazamiento);
	}

	private int xDeLaBarra() {
		return getX() + width - ANCHO_BARRA;
	}

	private int altoDeLaBarra() {
		return Math.max(ALTO_MINIMO_BARRA, height * height / Math.max(1, contenido.getHeight()));
	}

	@Override
	protected void renderWidget(GuiGraphics graficos, int mouseX, int mouseY, float parcial) {
		// Con el mouse fuera de la vista, un widget tapado no se marca ni muestra su tooltip
		boolean adentro = isMouseOver(mouseX, mouseY);
		graficos.enableScissor(getX(), getY(), getX() + width, getY() + height);
		for (AbstractWidget hijo : hijos) {
			hijo.render(graficos, adentro ? mouseX : -1, adentro ? mouseY : -1, parcial);
		}
		graficos.disableScissor();
		if (maximo() > 0) {
			int alto = altoDeLaBarra();
			int y = getY() + (int) (desplazamiento * (height - alto) / maximo());
			graficos.fill(xDeLaBarra(), getY(), xDeLaBarra() + ANCHO_BARRA, getY() + height, 0x80000000);
			graficos.fill(xDeLaBarra(), y, xDeLaBarra() + ANCHO_BARRA, y + alto, 0xFF808080);
			graficos.fill(xDeLaBarra(), y, xDeLaBarra() + ANCHO_BARRA - 1, y + alto - 1, 0xFFC0C0C0);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int boton) {
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		}
		if (maximo() > 0 && mouseX >= xDeLaBarra()) {
			arrastrandoBarra = true;
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, boton);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int boton, double dx, double dy) {
		if (arrastrandoBarra) {
			desplazar(dy * maximo() / Math.max(1, height - altoDeLaBarra()));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, boton, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int boton) {
		arrastrandoBarra = false;
		return super.mouseReleased(mouseX, mouseY, boton);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (maximo() == 0) {
			return false;
		}
		desplazar(-vertical * PASO_DE_LA_RUEDA);
		return true;
	}

	/** Con Tab, el campo que toma el foco se trae a la vista. */
	@Override
	public void setFocused(@Nullable GuiEventListener hijo) {
		super.setFocused(hijo);
		if (hijo instanceof AbstractWidget widget) {
			int arriba = widget.getY() - getY();
			int abajo = widget.getY() + widget.getHeight() - (getY() + height);
			if (arriba < 0) {
				desplazar(arriba);
			} else if (abajo > 0) {
				desplazar(abajo);
			}
		}
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return hijos;
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput salida) {
		// Cada widget de adentro narra lo suyo cuando tiene el foco
	}
}
