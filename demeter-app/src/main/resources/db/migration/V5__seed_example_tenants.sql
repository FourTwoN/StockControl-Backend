-- =============================================
-- V5: Seed example tenants for development
-- =============================================

INSERT INTO tenants (id, name, industry, theme, enabled_modules, settings) VALUES
(
    'go-bar',
    'Go Bar',
    'COMERCIANTES',
    '{
        "primary": "#FF6B35",
        "secondary": "#0F172A",
        "accent": "#E65100",
        "background": "#F8FAFC",
        "logoUrl": null,
        "appName": "Go Bar Stock"
    }'::jsonb,
    '["inventario", "productos", "ventas", "ubicaciones", "fotos"]'::jsonb,
    '{}'::jsonb
),
(
    'central-de-bebidas',
    'Central de Bebidas',
    'DISTRIBUIDORES',
    '{
        "primary": "#1E40AF",
        "secondary": "#1E293B",
        "accent": "#2563EB",
        "background": "#F1F5F9",
        "logoUrl": null,
        "appName": "Central de Bebidas"
    }'::jsonb,
    '["inventario", "productos", "ventas", "costos", "ubicaciones", "empaquetado", "precios", "analytics"]'::jsonb,
    '{}'::jsonb
);
