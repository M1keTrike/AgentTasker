# ================================
# Dependencies Stage
# ================================
FROM node:20-alpine AS deps

# Instalar pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

WORKDIR /app

# Copiar archivos de dependencias
COPY package.json pnpm-lock.yaml ./

# Instalar todas las dependencias (incluyendo dev)
RUN pnpm install --frozen-lockfile

# ================================
# Development Stage
# ================================
FROM deps AS development

# Copiar código fuente
COPY . .

ENV NODE_ENV=development
ENV PORT=3000

EXPOSE 3000

# Hot-reload con NestJS CLI
CMD ["pnpm", "run", "start:dev"]

# ================================
# Build Stage
# ================================
FROM deps AS builder

# Copiar código fuente
COPY . .

# Compilar la aplicación
RUN pnpm run build

# Eliminar dependencias de desarrollo
RUN pnpm prune --prod

# ================================
# Production Stage
# ================================
FROM node:20-alpine AS production

# Instalar pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

# Crear usuario no-root para seguridad
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nestjs -u 1001

WORKDIR /app

# Copiar archivos necesarios desde el builder
COPY --from=builder --chown=nestjs:nodejs /app/dist ./dist
COPY --from=builder --chown=nestjs:nodejs /app/node_modules ./node_modules
COPY --from=builder --chown=nestjs:nodejs /app/package.json ./

# Cambiar a usuario no-root
USER nestjs

# Exponer puerto
EXPOSE 3000

# Variables de entorno por defecto
ENV NODE_ENV=production
ENV PORT=3000

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD node -e "require('http').get('http://localhost:3000', (r) => process.exit(r.statusCode === 200 ? 0 : 1))"

# Comando de inicio
CMD ["node", "dist/main.js"]
