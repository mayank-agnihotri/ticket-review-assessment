type Props = {
  message?: string;
};

export function FieldError({ message }: Props) {
  if (!message) {
    return null;
  }
  return <span className="field-error">{message}</span>;
}
