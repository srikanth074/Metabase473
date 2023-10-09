import { t } from "ttag";

import ConfirmContent from "metabase/components/ConfirmContent";

interface Props {
  onAction?: () => void;
  onCancel?: () => void;
  onClose?: () => void;
}

export const LeaveConfirmation = ({ onAction, onCancel, onClose }: Props) => (
  <ConfirmContent
    data-testid="leave-confirmation"
    cancelButtonText={t`Cancel`}
    confirmButtonText={t`Leave anyway`}
    message={t`Navigating away from here will cause you to lose any changes you have made.`}
    title={t`Changes were not saved`}
    onAction={onAction}
    onCancel={onCancel}
    onClose={onClose}
  />
);
