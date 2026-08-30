"use client";

import { ArrowLeft, Box, Plus, Save, Users, X } from "lucide-react";
import type { ReactNode } from "react";

type Props = {
  title: string;
  icon?: "users" | "box" | ReactNode;
  onVoltar: () => void;
  onNovo?: () => void;
  onCancelar: () => void;
  onSalvar: () => void;
  saveDisabled?: boolean;
  showNovo?: boolean;
};

export function FiscalDetailToolbar({
  title,
  icon = "users",
  onVoltar,
  onNovo,
  onCancelar,
  onSalvar,
  saveDisabled,
  showNovo = true,
}: Props) {
  const Icon =
    icon === "box" ? (
      <Box className="h-5 w-5 text-slate-500" />
    ) : icon === "users" ? (
      <Users className="h-5 w-5 text-slate-500" />
    ) : (
      icon
    );

  return (
    <div className="fiscal-detail-toolbar">
      <div className="fiscal-detail-toolbar__start">
        <button
          type="button"
          className="fiscal-icon-btn"
          onClick={onVoltar}
          aria-label="Voltar à pesquisa"
        >
          <ArrowLeft className="h-5 w-5" />
        </button>
        {Icon}
      </div>
      <div className="fiscal-detail-toolbar__title-wrap">
        <span className="fiscal-detail-toolbar__title">{title}</span>
      </div>
      <div className="fiscal-detail-toolbar__actions">
        {showNovo && onNovo && (
          <button type="button" className="fiscal-toolbar-novo" onClick={onNovo}>
            <Plus className="h-4 w-4" /> Novo
          </button>
        )}
        <button type="button" className="fiscal-toolbar-cancelar" onClick={onCancelar}>
          <X className="h-4 w-4" /> Cancelar
        </button>
        <button
          type="button"
          className="fiscal-toolbar-salvar"
          disabled={saveDisabled}
          onClick={onSalvar}
        >
          <Save className="h-4 w-4" /> Salvar
        </button>
      </div>
    </div>
  );
}
